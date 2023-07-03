package no.nav.modia.soknadsstatus

fun main() {
    System.setProperty("APP_NAME", "data-generator-app")
    System.setProperty("APP_VERSION", "dev")
    System.setProperty("APP_MODE", "LOCALLY_WITHIN_IDEA")
    System.setProperty("JMS_CHANNEL", "arena-infotrygd-soknadsstatus")
    System.setProperty("JMS_HOST", "localhost")
    System.setProperty("JMS_PORT", "61616")
    System.setProperty("JMS_QUEUEMANAGER", "")
    System.setProperty("JMS_USERNAME", "")
    System.setProperty("JMS_PASSWORD", "")

    System.setProperty("KAFKA_SOURCE_TOPIC", "arena-soknadsstatus")
    System.setProperty("KAFKA_TARGET_TOPIC", "modia-soknadsstatus")
    System.setProperty("KAFKA_BROKERS", "localhost:9092")

    runApp(port = 9999)
}
