package no.nav.modia.soknadsstatus

fun main() {
    System.setProperty("APP_MODE", "LOCALLY_WITHIN_IDEA")
    System.setProperty("APP_NAME", "modia-soknadsstatus-api")
    System.setProperty("APP_VERSION", "dev")

    System.setProperty("KAFKA_SOURCE_TOPIC", "modia-soknadsstatus")
    System.setProperty("KAFKA_BROKERS", "localhost:9092")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_TOPIC", "modia-soknadsstatus-dlq")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_CONSUMER_POLL_INTERVAL_MS", "10000")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_SKIP_TABLE_NAME", "modia_soknadsstatus_dlq_event_skip")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_METRICS_GAUGE_NAME", "modia_soknadsstatus_api_dlq_gauge")

    System.setProperty("NAIS_DATABASE_MODIA_SOKNADSSTATUS_API_MODIA_SOKNADSSTATUS_HOST", "localhost")
    System.setProperty("NAIS_DATABASE_MODIA_SOKNADSSTATUS_API_MODIA_SOKNADSSTATUS_PORT", "5432")
    System.setProperty("NAIS_DATABASE_MODIA_SOKNADSSTATUS_API_MODIA_SOKNADSSTATUS_USERNAME", "admin")
    System.setProperty("NAIS_DATABASE_MODIA_SOKNADSSTATUS_API_MODIA_SOKNADSSTATUS_PASSWORD", "admin")
    System.setProperty("DB_NAME", "modia-soknadsstatus")
    System.setProperty("NAIS_CLUSTER_NAME", "dev-gcp")
    MockData.setupAzureAdLocally()
    MockData.setUpMocks()
    runApp(port = 9012)
}
