package no.nav.modia.soknadsstatus

import io.ktor.server.cio.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.nav.personoversikt.common.ktor.utils.KtorServer

fun main() {
    runApp()
}

fun runApp(port: Int = 8080) {
    val env = Env()
    val configuration = Configuration.factory(env)
    val services = Services.factory(env, configuration)
    env.datasourceConfiguration.runFlyway()

    GlobalScope.launch {
        AktorMigreringJob(services).start()
    }

    KtorServer
        .create(
            factory = CIO,
            port = port,
        ) {
            soknadsstatusModule(env, configuration, services)
        }.start(wait = true)
}
