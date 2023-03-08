package no.nav.modia.soknadsstatus

import io.ktor.server.cio.*
import no.nav.personoversikt.common.ktor.utils.KtorServer

fun main() {
    runApp()
}

fun runApp(port: Int = 8080, useMock: Boolean = false) {
    val env = Env()
    val configuration = ConfigurationImpl(env)
    val services = ServicesImpl(configuration)
    env.datasourceConfiguration.runFlyway()

    KtorServer.create(
        factory = CIO,
        port = port,
    ) {
        soknadsstatusModule(env, configuration, services, useMock = false)
    }.start(wait = true)
}
