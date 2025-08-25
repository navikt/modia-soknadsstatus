package no.nav.modia.soknadsstatus

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import no.nav.personoversikt.common.logging.Logging.TEAM_LOGS_MARKER
import no.nav.personoversikt.common.logging.Logging.teamLog

class WebStatusException(
    message: String,
    val status: HttpStatusCode,
) : Exception(message)

fun Application.configureExceptionHandling() {
    install(StatusPages) {
        exception<WebStatusException> { call, cause ->
            teamLog.warn(TEAM_LOGS_MARKER, "WebStatusException:${cause.status.value}", call)
            call.respond(
                cause.status,
                HttpErrorResponse(
                    message = cause.message,
                ),
            )
        }
        exception<Throwable> { call, cause ->
            teamLog.error(TEAM_LOGS_MARKER, "Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                HttpErrorResponse(
                    message = cause.message,
                ),
            )
        }
        status(HttpStatusCode.Unauthorized) { statusCode ->
            val message = call.authentication.allFailures.joinToString("\n") { it.prettyPrint() }
            teamLog.error(TEAM_LOGS_MARKER, message)
            call.respond(statusCode, message)
        }
    }
}

private fun AuthenticationFailedCause.prettyPrint(): String =
    when (this) {
        AuthenticationFailedCause.NoCredentials -> "No credentials"
        AuthenticationFailedCause.InvalidCredentials -> "Invalid credentials"
        is AuthenticationFailedCause.Error -> "Error with credentials: ${this.message}"
    }

@Serializable
internal data class HttpErrorResponse(
    val message: String? = null,
)
