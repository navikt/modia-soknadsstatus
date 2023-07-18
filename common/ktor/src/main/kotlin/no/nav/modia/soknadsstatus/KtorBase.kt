package no.nav.modia.soknadsstatus

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import no.nav.personoversikt.common.ktor.utils.Metrics
import no.nav.personoversikt.common.ktor.utils.Selftest

private val shutdownhooks: MutableList<() -> Unit> = mutableListOf()
fun registerShutdownhook(hook: () -> Unit) {
    shutdownhooks += hook
}

val BaseNaisApp = createApplicationPlugin("base-nais-app") {
    with(application) {
        install(Metrics.Plugin)
        install(Selftest.Plugin)

        configureExceptionHandling()

        install(ShutDownUrl.ApplicationCallPlugin) {
            shutDownUrl = "/shutdown"
            shutdownhooks.forEach { it.invoke() }
        }
    }
}

class HttpStatusException(
    val status: HttpStatusCode,
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

suspend inline fun <reified T : Any> ApplicationCall.respondWithResult(result: Result<T>) {
    result.fold(
        onSuccess = { respond(HttpStatusCode.OK, it) },
        onFailure = { throw HttpStatusException(HttpStatusCode.InternalServerError, "Internal server error", it) },
    )
}
