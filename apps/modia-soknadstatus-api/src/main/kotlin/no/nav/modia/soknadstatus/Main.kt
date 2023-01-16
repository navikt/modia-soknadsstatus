package no.nav.modia.soknadstatus

import io.ktor.server.cio.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
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
            kafkaStreamModule(config) {
                stream<String, String>(config.sourceTopic)
                    .mapValues(::deserialize)
                    .filter(::filter)
                    .foreach(::persist)
            }
        }
    ).start(wait = true)
}

fun deserialize(key: String?, value: String): SoknadstatusDomain.Soknadstatus {
    return Json.decodeFromString(value)
}

fun filter(key: String?, value: SoknadstatusDomain.Soknadstatus): Boolean {
    return true
}

fun persist(bytes: String?, soknadstatus: SoknadstatusDomain.Soknadstatus) {
    TODO("Not yet implemented")
}
