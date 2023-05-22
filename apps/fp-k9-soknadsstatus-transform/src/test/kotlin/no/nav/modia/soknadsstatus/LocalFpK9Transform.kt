package no.nav.modia.soknadsstatus

fun main() {
    System.setProperty("APP_NAME", "fp-k9-transform")
    System.setProperty("APP_VERSION", "dev")

    System.setProperty("KAFKA_SOURCE_TOPIC", "aapen-sob-oppgaveHendelse-v1")
    System.setProperty("KAFKA_TARGET_TOPIC", "modia-soknadsstatus")
    System.setProperty("KAFKA_BROKER_URL", "localhost:9092")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_TOPIC", "aapen-sob-oppgaveHendelse-v1-dlq")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_CONSUMER_POLL_INTERVAL_MS", "10000")
    System.setProperty("KAFKA_DEAD_LETTER_SKIP_TABLE_NAME", "fp_k9_dlq_event_skip")

    System.setProperty("JDBC_URL", "jdbc:postgresql://localhost:5432/modia-soknadsstatus")
    System.setProperty("JDBC_USERNAME", "admin")
    System.setProperty("JDBC_PASSWORD", "admin")
    runApp(port = 9020)
}
