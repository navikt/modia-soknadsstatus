package no.nav.modia.soknadsstatus

fun main() {
    System.setProperty("APP_NAME", "arena-infotrygd-soknadsstatus-transform")
    System.setProperty("APP_VERSION", "dev")

    System.setProperty("KAFKA_SOURCE_TOPIC", "arena-infotrygd-soknadsstatus")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_TOPIC", "arena-infotrygd-soknadsstatus-dlq")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_CONSUMER_POLL_INTERVAL_MS", "60000")
    System.setProperty("KAFKA_TARGET_TOPIC", "modia-soknadsstatus")
    System.setProperty("KAFKA_BROKER_URL", "localhost:9092")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_METRICS_GAUGE_NAME", "modia_soknadsstatus_arena_infotrygd_dlq_gauge")

    runApp(port = 9010)
}
