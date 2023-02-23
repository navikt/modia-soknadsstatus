package no.nav.modia.soknadsstatus

fun main() {
    System.setProperty("APP_NAME", "fp-k9-transform")
    System.setProperty("APP_VERSION", "dev")

    System.setProperty("KAFKA_SOURCE_TOPIC", "aapen-sob-oppgaveHendelse-v1")
    System.setProperty("KAFKA_TARGET_TOPIC", "modia-soknadsstatus")
    System.setProperty("KAFKA_BROKER_URL", "localhost:9092")

    runApp(port = 9011)
}
