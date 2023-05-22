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
import org.apache.kafka.common.serialization.Serdes.StringSerde
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler

fun Application.dataGeneratorModule() {
    val config = Configuration()
    val isLocal = true
    val wsConnections = mutableListOf<DefaultWebSocketSession>()

    val env = object : AppEnv {
        override val appName = "data-generator"
        override val appMode = no.nav.modia.soknadsstatus.AppMode.LOCALLY_WITHIN_DOCKER
        override val appVersion = "test"
        override val brokerUrl = config.brokerUrl
        override val sourceTopic = config.soknadsstatusTopic
        override val targetTopic: String? = null
        override val deadLetterQueueTopic: String? = null
        override val deadLetterQueueConsumerPollIntervalMs: Double = 10000.0
        override val deadLetterQueueSkipTableName: String? = null
        override val deadLetterQueueMetricsGaugeName: String? = null
    }

    val handlers = Handlers(env).handlers

    install(KafkaStreamPlugin<String>()) {
        appEnv = env
        deserializationExceptionHandler = LogAndContinueExceptionHandler()
        valueSerde = StringSerde()

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
