package no.nav.modia.soknadstatus

fun main() {
    System.setProperty("APP_NAME", "infotrygd-soknadstatus-transform")
    System.setProperty("APP_VERSION", "dev")

    System.setProperty("KAFKA_SOURCE_TOPIC", "infotrygd-soknadstatus")
    System.setProperty("KAFKA_TARGET_TOPIC", "modia-soknadstatus")
    System.setProperty("KAFKA_BROKER_URL", "localhost:9092")

    runApp(port = 9010)
}
