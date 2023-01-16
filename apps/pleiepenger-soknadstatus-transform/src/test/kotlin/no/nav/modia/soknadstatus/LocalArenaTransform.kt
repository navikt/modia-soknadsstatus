package no.nav.modia.soknadstatus

fun main() {
    System.setProperty("APP_NAME", "pleiepenger-soknadstatus-transform")
    System.setProperty("APP_VERSION", "dev")

    System.setProperty("KAFKA_SOURCE_TOPIC", "pleiepenger-soknadstatus")
    System.setProperty("KAFKA_TARGET_TOPIC", "modia-soknadstatus")
    System.setProperty("KAFKA_BROKER_URL", "localhost:9092")

    runApp(port = 9011)
}
