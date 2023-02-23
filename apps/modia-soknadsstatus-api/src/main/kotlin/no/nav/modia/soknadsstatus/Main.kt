package no.nav.modia.soknadsstatus

import io.ktor.server.cio.*
import no.nav.personoversikt.common.ktor.utils.KtorServer

fun main() {
    runApp()
}

fun runApp(port: Int = 8080, useMock: Boolean = false) {
    val configuration = Configuration()
    val repository = soknadsstatusRepository(configuration.datasourceConfiguration.datasource)
    configuration.datasourceConfiguration.runFlyway()

    KtorServer.create(
        factory = CIO,
        port = port,
    ) {
        soknadsstatusModule(configuration, repository, useMock)
    }.start(wait = true)
}
