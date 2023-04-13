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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import no.nav.common.types.identer.Fnr
import no.nav.modia.soknadsstatus.accesscontrol.kabac.Policies
import no.nav.modia.soknadsstatus.infratructure.naudit.Audit
import no.nav.modia.soknadsstatus.infratructure.naudit.AuditResources
import no.nav.modiapersonoversikt.infrastructure.naudit.AuditIdentifier
import no.nav.personoversikt.common.ktor.utils.Security
import no.nav.personoversikt.common.logging.Logging.secureLog
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
        if (env.appMode == AppMode.NAIS) {
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

    install(KafkaStreamPlugin) {
        appname = env.appName
        brokerUrl = env.brokerUrl
        topology {
            stream<String, String>(env.sourceTopic)
                .mapValues(::deserialize)
                .foreach { key, value ->
                    services.soknadsstatusService.fetchIdentsAndPersist(value)
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
                                    services.soknadsstatusService.fetchDataForIdent(ident)
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
                                    services.soknadsstatusService.fetchAggregatedDataForIdent(ident)
                                }
                        )
                    }
                }
            }
        }
    }
}

fun deserialize(key: String?, value: String): SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering? {
    return try {
        Json.decodeFromString(value)
    } catch (e: Exception) {
        secureLog.error("Failed to decode message", e)
        null
    }
}
