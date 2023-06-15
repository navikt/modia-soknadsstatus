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
import no.nav.common.types.identer.Fnr
import no.nav.modia.soknadsstatus.accesscontrol.kabac.Policies
import no.nav.modia.soknadsstatus.infratructure.naudit.Audit
import no.nav.modia.soknadsstatus.infratructure.naudit.AuditResources
import no.nav.modia.soknadsstatus.kafka.*
import no.nav.modia.soknadsstatus.utils.getTokenFromPrincipal
import no.nav.modiapersonoversikt.infrastructure.naudit.AuditIdentifier
import no.nav.personoversikt.common.ktor.utils.Security
import org.apache.kafka.common.serialization.Serdes.StringSerde
import org.slf4j.event.Level

fun Application.soknadsstatusModule(
    env: Env = Env(),
    configuration: Configuration = Configuration.factory(env),
    services: Services = Services.factory(env, configuration),
) {
    val security = Security(
        listOfNotNull(
            configuration.azureAd
        )
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
            security.setupMock(this, "Z999999")
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

    install(KafkaStreamPlugin<SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering>()) {
        appEnv = env.kafkaApp
        valueSerde = SoknadsstatusDomain.SoknadsstatusInkommendeOppdateringSerde()
        deserializationExceptionHandler =
            SendToDeadLetterQueueExceptionHandler()
        deadLetterQueueProducer = services.dlqProducer
        topology {
            stream<String, SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering>(env.kafkaApp.sourceTopic)
                .foreach { key, value ->
                    try {
                        services.soknadsstatusService.fetchIdentsAndPersist(value)
                    } catch (e: Exception) {
                        val encodedValue =
                            Encoding.encode(SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering.serializer(), value)
                        services.dlqProducer.sendMessage(
                            key,
                            encodedValue
                        )
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
                    StringSerde()
                ),
                pollDurationMs = env.kafkaApp.deadLetterQueueConsumerPollIntervalMs,
                deadLetterMessageSkipService = services.dlSkipService,
                deadLetterQueueMetricsGauge = DeadLetterQueueMetricsGaugeImpl(requireNotNull(env.kafkaApp.deadLetterQueueMetricsGaugeName))
            ) { _, _, value ->
                kotlin.runCatching {
                    val inkommendeOppdatering =
                        Encoding.decode(SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering.serializer(), value)
                    services.soknadsstatusService.fetchIdentsAndPersist(inkommendeOppdatering)
                }
            }
    }

    routing {
        authenticate(*security.authproviders) {
            route("api") {
                route("soknadsstatus") {
                    get("oppdateringer/{ident}") {
                        val kabac = services.accessControl.buildKabac(call.authentication)
                        val ident = call.parameters["ident"] ?: throw HttpStatusException(
                            HttpStatusCode.BadRequest,
                            "ident missing in request"
                        )
                        call.respondWithResult(
                            kabac
                                .check(Policies.tilgangTilBruker(Fnr(ident))).get(
                                    Audit.describe(
                                        call.authentication,
                                        Audit.Action.READ,
                                        AuditResources.Person.SakOgBehandling.Les,
                                        AuditIdentifier.FNR to ident
                                    )
                                ) {
                                    services.soknadsstatusService.fetchDataForIdent(
                                        getTokenFromPrincipal(call.authentication),
                                        ident
                                    )
                                }
                        )
                    }

                    get("{ident}") {
                        val ident = call.parameters["ident"] ?: throw HttpStatusException(
                            HttpStatusCode.BadRequest,
                            "ident missing in request"
                        )
                        val kabac = services.accessControl.buildKabac(call.authentication)
                        call.respondWithResult(
                            kabac
                                .check(Policies.tilgangTilBruker(Fnr(ident))).get(
                                    Audit.describe(
                                        call.authentication,
                                        Audit.Action.READ,
                                        AuditResources.Person.SakOgBehandling.Les,
                                        AuditIdentifier.FNR to ident,
                                    )
                                ) {
                                    services.soknadsstatusService.fetchAggregatedDataForIdent(
                                        getTokenFromPrincipal(call.authentication),
                                        ident
                                    )
                                }
                        )
                    }
                }
            }
        }
    }
}
