package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import io.ktor.server.cio.*
import no.nav.personoversikt.common.ktor.utils.KtorServer

fun main() {
    runApp()
}

fun runApp(port: Int = 8080) {
    KtorServer
        .create(
            factory = CIO,
            port = port,
            application = Application::dataGeneratorModule,
        ).start(wait = true)
}
