package no.nav.modia.soknadstatus

import io.ktor.server.application.*
import io.ktor.server.cio.*
import no.nav.personoversikt.common.ktor.utils.KtorServer

fun main() {
    runApp()
}

val databaseConfiguration = DatasourceConfiguration()
val repository = SoknadstatusRepository(databaseConfiguration.datasource)
fun runApp(port: Int = 8080) {
    databaseConfiguration.runFlyway()

    KtorServer.create(
        factory = CIO,
        port = port,
        application = Application::soknadstatusModule
    ).start(wait = true)
}
