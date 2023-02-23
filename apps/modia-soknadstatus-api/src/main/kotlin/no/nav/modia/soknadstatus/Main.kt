package no.nav.modia.soknadstatus

import io.ktor.server.application.*
import io.ktor.server.cio.*
import no.nav.personoversikt.common.ktor.utils.KtorServer

fun main() {
    runApp()
}

fun runApp(port: Int = 8080, useMock: Boolean = false) {
    val configuration = Configuration()
    val repository = SoknadstatusRepository(configuration.datasourceConfiguration.datasource)
    configuration.datasourceConfiguration.runFlyway()

    KtorServer.create(
        factory = CIO,
        port = port,
    ) {
        soknadstatusModule(configuration, repository, useMock)
    }.start(wait = true)
}
