package no.nav.modia.soknadsstatus

fun main() {
    System.setProperty("APP_NAME", "infotrygd-soknadsstatus-transform")
    System.setProperty("APP_VERSION", "dev")

    System.setProperty("KAFKA_SOURCE_TOPIC", "infotrygd-soknadsstatus")
    System.setProperty("KAFKA_TARGET_TOPIC", "modia-soknadsstatus")
    System.setProperty("KAFKA_BROKER_URL", "localhost:9092")

    runApp(port = 9010)
}
