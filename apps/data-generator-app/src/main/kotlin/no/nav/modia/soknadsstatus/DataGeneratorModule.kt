package no.nav.modia.soknadsstatus

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
import no.nav.modia.soknadsstatus.kafka.AppEnv

fun Application.dataGeneratorModule() {
    val isLocal = true
    val wsConnections = mutableListOf<DefaultWebSocketSession>()

    val env =
        AppEnv(
            appName = "data-generator",
            appMode = AppMode.LOCALLY_WITHIN_DOCKER,
            appVersion = "test",
            deadLetterQueueTopic = null,
            deadLetterQueueConsumerPollIntervalMs = 10000.0,
            deadLetterQueueSkipTableName = null,
            deadLetterQueueMetricsGaugeName = null,
        )

    val config = Configuration(env)
    val handlers = Handlers(env).handlers

    install(KafkaStreamPlugin()) {
        appEnv = env

        topology {
            stream<String, String>(config.soknadsstatusTopic)
                .foreach { _, value ->
                    runBlocking {
                        val frame = Frame.Text(value)
                        for (wsConnection in wsConnections) {
                            wsConnection.send(frame)
                        }
                    }
                }
        }
    }

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
                    requireNotNull(handlers[kilde.type])(kilde, content)

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
