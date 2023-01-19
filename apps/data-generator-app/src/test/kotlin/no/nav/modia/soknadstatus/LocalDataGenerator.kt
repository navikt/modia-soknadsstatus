package no.nav.modia.soknadstatus

fun main() {
    System.setProperty("APP_NAME", "data-generator-app")
    System.setProperty("APP_VERSION", "dev")

    System.setProperty("JMS_QUEUE", "arena-soknadstatus")
    System.setProperty("JMS_HOST", "localhost")
    System.setProperty("JMS_PORT", "61616")
    System.setProperty("JMS_QUEUEMANAGER", "")
    System.setProperty("JMS_USERNAME", "")
    System.setProperty("JMS_PASSWORD", "")

    System.setProperty("KAFKA_SOURCE_TOPIC", "arena-soknadstatus")
    System.setProperty("KAFKA_TARGET_TOPIC", "modia-soknadstatus")
    System.setProperty("KAFKA_BROKER_URL", "localhost:9092")

    runApp(port = 9999)
}
