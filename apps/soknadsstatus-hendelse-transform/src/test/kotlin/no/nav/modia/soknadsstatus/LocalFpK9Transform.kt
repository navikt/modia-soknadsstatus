package no.nav.modia.soknadsstatus

fun main() {
    System.setProperty("APP_NAME", "soknadsstatus-hendelse-transform")
    System.setProperty("APP_VERSION", "dev")
    System.setProperty("APP_MODE", "LOCALLY_WITHIN_IDEA")

    System.setProperty("KAFKA_SOURCE_TOPIC", "aapen-sob-oppgaveHendelse-v1")
    System.setProperty("KAFKA_TARGET_TOPIC", "modia-soknadsstatus")
    System.setProperty("KAFKA_BROKERS", "localhost:9092")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_TOPIC", "aapen-sob-oppgaveHendelse-v1-dlq")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_CONSUMER_POLL_INTERVAL_MS", "60000")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_SKIP_TABLE_NAME", "fp_k9_dlq_event_skip")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_METRICS_GAUGE_NAME", "modia_soknadsstatus_fp_k9_dlq_gauge")

    System.setProperty("NAIS_DATABASE_FP_K9_SOKNADSSTATUS_TRANSFORM_MODIA_SOKNADSSTATUS_HOST", "localhost")
    System.setProperty("NAIS_DATABASE_FP_K9_SOKNADSSTATUS_TRANSFORM_MODIA_SOKNADSSTATUS_PORT", "5432")
    System.setProperty("NAIS_DATABASE_FP_K9_SOKNADSSTATUS_TRANSFORM_MODIA_SOKNADSSTATUS_USERNAME", "admin")
    System.setProperty("NAIS_DATABASE_FP_K9_SOKNADSSTATUS_TRANSFORM_MODIA_SOKNADSSTATUS_PASSWORD", "admin")
    System.setProperty("DB_NAME", "modia-soknadsstatus")
    runApp(port = 9020)
}
