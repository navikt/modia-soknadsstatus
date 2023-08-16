package no.nav.modia.soknadsstatus

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import no.nav.common.types.identer.Fnr
import no.nav.modia.soknadsstatus.infratructure.naudit.Audit
import no.nav.modia.soknadsstatus.infratructure.naudit.AuditResources
import no.nav.modia.soknadsstatus.kafka.*
import no.nav.modiapersonoversikt.infrastructure.naudit.AuditIdentifier
import no.nav.personoversikt.common.ktor.utils.Security
import no.nav.personoversikt.common.logging.TjenestekallLogg
import org.slf4j.event.Level

fun Application.soknadsstatusModule(
    env: Env = Env(),
    configuration: Configuration = Configuration.factory(env),
    services: Services = Services.factory(env, configuration),
) {
    val security = Security(
        listOfNotNull(
            configuration.authProviderConfig,
        ),
    )

    install(CORS) {
        anyHost() // TODO kanskje kun whiteliste personoversikt domenene i første omgang?
        allowMethod(HttpMethod.Get)
    }

    install(BaseNaisApp)

    install(Authentication) {
        if (env.kafkaApp.appMode == AppMode.NAIS) {
            security.setupJWT(this)
        } else {
            security.setupMock(this, "Z999999", "f24261ea-60ed-4894-a2d6-29e55214df08")
        }
    }

    install(ContentNegotiation) {
        json()
    }

    install(CallLogging) {
        level = Level.INFO
        disableDefaultColors()
        filter { call -> call.request.path().startsWith("/modia-soknadsstatus/api") }
        mdc("userId") { security.getSubject(it).joinToString(";") }
    }

    install(KafkaStreamPlugin()) {
        appEnv = env.kafkaApp
        topology {
            stream<String, String>(env.kafkaApp.sourceTopic)
                .foreach { key, value ->
                    runBlocking {
                        try {
                            val decodedValue = Encoding.decode(
                                SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering.serializer(),
                                value
                            )
                            async(Dispatchers.IO) {
                                services.soknadsstatusService.persistUpdate(decodedValue)
                            }.await()
                        } catch (e: Exception) {
                            services.dlqProducer.sendMessage(
                                key,
                                value,
                            )
                        }
                    }
                }
        }
    }

    install(DeadLetterQueueConsumerPlugin()) {
        deadLetterQueueConsumer =
            DeadLetterQueueConsumerImpl(
                topic = requireNotNull(env.kafkaApp.deadLetterQueueTopic),
                kafkaConsumer = KafkaUtils.createConsumer(
                    env.kafkaApp,
                ),
                pollDurationMs = env.kafkaApp.deadLetterQueueConsumerPollIntervalMs,
                exceptionRestartDelayMs = env.kafkaApp.deadLetterQueueExceptionRestartDelayMs,
                deadLetterMessageSkipService = services.dlSkipService,
                deadLetterQueueMetricsGauge = DeadLetterQueueMetricsGaugeImpl(requireNotNull(env.kafkaApp.deadLetterQueueMetricsGaugeName)),
            ) { _, key, value ->
                runCatching {
                    try {
                        val decodedValue =
                            Encoding.decode(SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering.serializer(), value)
                        services.soknadsstatusService.persistUpdate(decodedValue)
                    } catch (e: Exception) {
                        TjenestekallLogg.error(
                            "Klarte ikke å håndtere DL",
                            fields = mapOf("key" to key, "value" to value),
                            throwable = e
                        )
                        throw e
                    }
                }
            }
    }

    routing {
        authenticate(*security.authproviders) {
            route("api") {
                route("soknadsstatus") {
                    get("oppdateringer/{ident}") {
                        val kabac = services.accessControl.buildKabac(call.authentication)
                        val ident = call.getIdent()
                        call.respondWithResult(
                            kabac
                                .check(services.policies.tilgangTilBruker(Fnr(ident))).get(
                                    Audit.describe(
                                        call.authentication,
                                        Audit.Action.READ,
                                        AuditResources.Person.SakOgBehandling.Les,
                                        AuditIdentifier.FNR to ident,
                                    ),
                                ) {
                                    services.soknadsstatusService.fetchDataForIdent(userToken = call.getUserToken(), ident)
                                },
                        )
                    }

                    get("{ident}") {
                        val ident = call.getIdent()
                        val kabac = services.accessControl.buildKabac(call.authentication)
                        call.respondWithResult(
                            kabac
                                .check(services.policies.tilgangTilBruker(Fnr(ident))).get(
                                    Audit.describe(
                                        call.authentication,
                                        Audit.Action.READ,
                                        AuditResources.Person.SakOgBehandling.Les,
                                        AuditIdentifier.FNR to ident,
                                    ),
                                ) {
                                    services.soknadsstatusService.fetchAggregatedDataForIdent(call.getUserToken(), ident)
                                },
                        )
                    }
                }
            }
        }
    }
}

private fun ApplicationCall.getIdent(): String {
    return this.parameters["ident"] ?: throw HttpStatusException(
        HttpStatusCode.BadRequest,
        "ident missing in request",
    )
}


private fun ApplicationCall.getUserToken(): String {
    return this.principal<Security.SubjectPrincipal>()?.token?.removeBearerFromToken()
        ?: throw IllegalStateException("Ingen gyldig token funnet")
}