package no.nav.modia.soknadsstatus

fun main() {
    System.setProperty("APP_NAME", "modia-soknadsstatus-api")
    System.setProperty("APP_VERSION", "dev")
    System.setProperty("KAFKA_SOURCE_TOPIC", "modia-soknadsstatus")
    System.setProperty("KAFKA_BROKER_URL", "localhost:9092")
    System.setProperty("JDBC_URL", "jdbc:postgresql://localhost:5432/modia-soknadsstatus")
    System.setProperty("JDBC_USERNAME", "admin")
    System.setProperty("JDBC_PASSWORD", "admin")

    runApp(port = 9012, useMock = true)
}
