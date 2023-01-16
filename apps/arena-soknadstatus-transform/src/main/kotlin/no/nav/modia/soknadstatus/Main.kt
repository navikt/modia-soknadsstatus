package no.nav.modia.soknadstatus

import io.ktor.server.cio.*
import kotlinx.datetime.Clock
import no.nav.personoversikt.common.ktor.utils.KtorServer

fun main() {
    runApp()
}

fun runApp(port: Int = 8080) {
    val config = Configuration()
    KtorServer.create(
        factory = CIO,
        port = port,
        application = {
            kafkaStreamTransformModule(config) { stream ->
                stream
                    .filter(::filter)
                    .mapValues(::transform)
            }
        }
    ).start(wait = true)
}

fun filter(key: String?, value: String): Boolean {
    return true
}

fun transform(key: String?, value: String): SoknadstatusDomain.Soknadstatus {
    // TODO fix mapping
    return SoknadstatusDomain.Soknadstatus(
        fnr = "123",
        tema = "DAG",
        status = SoknadstatusDomain.Status.UNDER_BEHANDLING,
        tidspunkt = Clock.System.now()
    )
}
