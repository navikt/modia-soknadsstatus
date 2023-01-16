package no.nav.modia.soknadstatus

import io.ktor.server.application.*
import io.ktor.server.cio.*
import no.nav.personoversikt.common.ktor.utils.KtorServer

const val port = 8080

fun main() {
    KtorServer.create(
        factory = CIO,
        port = port,
        application = Application::mqToKafkaModule
    ).start(wait = true)
}
