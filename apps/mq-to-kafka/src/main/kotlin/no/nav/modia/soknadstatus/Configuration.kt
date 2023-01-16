package no.nav.modia.soknadstatus

import no.nav.modia.soknadstatus.jms.Jms
import no.nav.personoversikt.common.utils.EnvUtils.getRequiredConfig

data class Configuration(
    val appname: String = getRequiredConfig("APP_NAME"),
    val appversion: String = getRequiredConfig("APP_VERSION"),
    val mqQueue: String = getRequiredConfig("JMS_QUEUE"),
    val kafkaTopic: String = getRequiredConfig("KAFKA_TOPIC"),
    val mqConfiguration: Jms.Config = create(),
    val kafkaConfiguration: KafkaConfiguration = KafkaConfiguration(),
) {

    data class KafkaConfiguration(
        val brokerUrl: String = getRequiredConfig("KAFKA_BROKER_URL"),
    )
}
private fun create() = Jms.Config(
    host = getRequiredConfig("JMS_HOST"),
    port = getRequiredConfig("JMS_PORT").toInt(),
    queueManager = getRequiredConfig("JMS_QUEUEMANAGER"),
    username = getRequiredConfig("JMS_USERNAME"),
    password = getRequiredConfig("JMS_PASSWORD"),
)
