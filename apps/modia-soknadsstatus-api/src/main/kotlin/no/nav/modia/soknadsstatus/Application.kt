package no.nav.modia.soknadsstatus

import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import no.nav.personoversikt.common.ktor.utils.Security
import no.nav.personoversikt.common.logging.Logging.secureLog
import org.slf4j.event.Level

fun Application.soknadsstatusModule(
    env: Env = Env(),
    configuration: Configuration = ConfigurationImpl(env),
    services: Services = ServicesImpl(configuration),
    useMock: Boolean
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
        if (useMock) {
            security.setupMock(this, "Z999999")
        } else {
            security.setupJWT(this)
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
                    services.soknadsstatusService.fetchIdentsAndPersist(value, "TODO")
                }
        }
    }

    routing {
        route("api") {
            route("soknadsstatus") {
                get("oppdateringer/{ident}") {
                    val ident = call.parameters["ident"] ?: throw HttpStatusException(
                        HttpStatusCode.BadRequest,
                        "ident missing in request"
                    )
                    call.respondWithResult(services.soknadsstatusService.fetchDataForIdent(ident))
                }

                get("{ident}") {
                    val ident = call.parameters["ident"] ?: throw HttpStatusException(
                        HttpStatusCode.BadRequest,
                        "ident missing in request"
                    )
                    call.respondWithResult(services.soknadsstatusService.fetchAggregatedDataForIdent(ident))
                }
            }
        }
    }
}

fun deserialize(key: String?, value: String): soknadsstatusDomain.soknadsstatusInnkommendeOppdatering? {
    return try {
        Json.decodeFromString(value)
    } catch (e: Exception) {
        secureLog.error("Failed to decode message", e)
        null
    }
}
