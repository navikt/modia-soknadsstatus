package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import io.ktor.server.cio.*
import no.nav.personoversikt.common.ktor.utils.KtorServer

fun main() {
    System.setProperty("APP_NAME", "arena-mq-to-kafka")
    System.setProperty("APP_VERSION", "dev")

    System.setProperty("KAFKA_TOPIC", "arena-soknadsstatus")
    System.setProperty("KAFKA_BROKER_URL", "localhost:9092")

    System.setProperty("JMS_QUEUE", "arena-soknadsstatus")
    System.setProperty("JMS_HOST", "localhost")
    System.setProperty("JMS_PORT", "61616")
    System.setProperty("JMS_QUEUEMANAGER", "")
    System.setProperty("JMS_USERNAME", "")
    System.setProperty("JMS_PASSWORD", "")

    KtorServer.create(
        factory = CIO,
        port = 9001,
        application = Application::mqToKafkaModule
    ).start(wait = true)
}
