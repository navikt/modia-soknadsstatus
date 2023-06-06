package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import io.ktor.server.cio.*
import no.nav.personoversikt.common.ktor.utils.KtorServer

fun main() {
    System.setProperty("APP_NAME", "arena-mq-to-kafka")
    System.setProperty("APP_VERSION", "dev")

    System.setProperty("KAFKA_TARGET_TOPIC", "arena-infotrygd-soknadsstatus")
    System.setProperty("KAFKA_BROKERS", "localhost:9092")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_TOPIC", "arena-infotrygd-soknadsstatus-dlq")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_CONSUMER_POLL_INTERVAL_MS", "10000")

    System.setProperty("JDBC_URL", "jdbc:postgresql://localhost:5432/modia-soknadsstatus")
    System.setProperty("JDBC_USERNAME", "admin")
    System.setProperty("JDBC_PASSWORD", "admin")

    System.setProperty("JMS_CHANNEL", "arena-soknadsstatus")
    System.setProperty("JMS_HOST", "localhost")
    System.setProperty("JMS_PORT", "61616")
    System.setProperty("JMS_QUEUE_MANAGER", "")
    System.setProperty("JMS_USERNAME", "")
    System.setProperty("JMS_PASSWORD", "")

    KtorServer.create(
        factory = CIO,
        port = 9001,
        application = Application::mqToKafkaModule
    ).start(wait = true)
}
