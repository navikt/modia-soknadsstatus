package no.nav.modia.soknadsstatus

fun main() {
    System.setProperty("APP_NAME", "soknadsstatus-hendelse-transform")
    System.setProperty("APP_VERSION", "dev")
    System.setProperty("APP_MODE", "LOCALLY_WITHIN_IDEA")

    System.setProperty("KAFKA_SOURCE_TOPIC", "personoversikt.modia-soknadsstatus-hendelse")
    System.setProperty("KAFKA_TARGET_TOPIC", "personoversikt.modia-soknadsstatus-oppdatering")
    System.setProperty("KAFKA_BROKERS", "localhost:9092")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_TOPIC", "personoversikt.modia-soknadsstatus-hendelse-dlq")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_CONSUMER_POLL_INTERVAL_MS", "10000")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_SKIP_TABLE_NAME", "hendelse_dlq_event_skip")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_METRICS_GAUGE_NAME", "modia_soknadsstatus_hendelse_dlq_gauge")

    System.setProperty("NAIS_DATABASE_SOKNADSSTATUS_HENDELSE_TRANSFORM_MODIA_SOKNADSSTATUS_HOST", "localhost")
    System.setProperty("NAIS_DATABASE_SOKNADSSTATUS_HENDELSE_TRANSFORM_MODIA_SOKNADSSTATUS_PORT", "5434")
    System.setProperty("NAIS_DATABASE_SOKNADSSTATUS_HENDELSE_TRANSFORM_MODIA_SOKNADSSTATUS_USERNAME", "admin")
    System.setProperty("NAIS_DATABASE_SOKNADSSTATUS_HENDELSE_TRANSFORM_MODIA_SOKNADSSTATUS_PASSWORD", "admin")
    System.setProperty("DB_NAME", "modia-soknadsstatus")
    runApp(port = 9020)
}
