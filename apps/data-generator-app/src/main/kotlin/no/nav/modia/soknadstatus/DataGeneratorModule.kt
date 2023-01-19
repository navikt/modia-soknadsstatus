package no.nav.modia.soknadstatus

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.runBlocking
import no.nav.modia.soknadstatus.kafka.KafkaUtils

fun Application.dataGeneratorModule() {
    val config = Configuration()
    val isLocal = true
    val wsConnections = mutableListOf<DefaultWebSocketSession>()

    KafkaUtils.createStream("data-generator", config.brokerUrl) {
        stream<String, String>(config.soknadstatusTopic)
            .foreach { _, value ->
                runBlocking {
                    val frame = Frame.Text(value)
                    for (wsConnection in wsConnections) {
                        wsConnection.send(frame)
                    }
                }
            }
    }.start()

    install(ContentNegotiation) {
        json()
    }
    install(WebSockets)

    routing {
        route("api") {
            route("kilder") {
                get {
                    call.respond(config.sources)
                }

                post("{identifier}") {
                    val identifier = call.parameters["identifier"]
                    val kilde = config.sources.find { it.resourceId == identifier }
                    if (kilde == null) {
                        call.respond(HttpStatusCode.NotFound, "Ingen kilde med id; $identifier")
                        return@post
                    }
                    val content = call.receive<String>()
                    requireNotNull(config.handlers[kilde.type])(kilde, content)

                    call.respond(HttpStatusCode.OK)
                }
            }
            webSocket("ws") {
                wsConnections += this
                try {
                    this.incoming.receive() // Keep connection open
                } catch (_: ClosedReceiveChannelException) {
                } finally {
                    wsConnections -= this
                }
            }
        }

        singlePageApplication {
            if (isLocal) {
                filesPath = "apps/data-generator-app/src/main/resources/www"
            } else {
                useResources = true
                filesPath = "www"
            }
        }
    }
}
