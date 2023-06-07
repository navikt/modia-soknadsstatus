package no.nav.modia.soknadsstatus

fun main() {
    System.setProperty("APP_NAME", "arena-infotrygd-soknadsstatus-transform")
    System.setProperty("APP_VERSION", "dev")
    System.setProperty("APP_MODE", "LOCALLY_WITHIN_IDEA")

    System.setProperty("KAFKA_SOURCE_TOPIC", "arena-infotrygd-soknadsstatus")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_TOPIC", "arena-infotrygd-soknadsstatus-dlq")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_CONSUMER_POLL_INTERVAL_MS", "60000")
    System.setProperty("KAFKA_TARGET_TOPIC", "modia-soknadsstatus")
    System.setProperty("KAFKA_BROKERS", "localhost:9092")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_SKIP_TABLE_NAME", "arena_infotrygd_dlq_event_skip")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_METRICS_GAUGE_NAME", "modia_soknadsstatus_arena_infotrygd_dlq_gauge")

    System.setProperty("NAIS_DATABASE_ARENA_INFOTRYGD_SOKNADSSTATUS_TRANSFORM_MODIA_SOKNADSSTATUS_HOST", "localhost")
    System.setProperty("NAIS_DATABASE_ARENA_INFOTRYGD_SOKNADSSTATUS_TRANSFORM_MODIA_SOKNADSSTATUS_PORT", "5432")
    System.setProperty("NAIS_DATABASE_ARENA_INFOTRYGD_SOKNADSSTATUS_TRANSFORM_MODIA_SOKNADSSTATUS_USERNAME", "admin")
    System.setProperty("NAIS_DATABASE_ARENA_INFOTRYGD_SOKNADSSTATUS_TRANSFORM_MODIA_SOKNADSSTATUS_PASSWORD", "admin")
    System.setProperty("DB_NAME", "modia-soknadsstatus")

    runApp(port = 9010)
}
