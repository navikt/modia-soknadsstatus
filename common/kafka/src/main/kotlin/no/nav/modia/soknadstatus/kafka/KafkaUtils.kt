package no.nav.modia.soknadstatus.kafka

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

object KafkaUtils {
    fun createProducer(
        applicationId: String,
        brokerUrl: String,
    ): KafkaProducer<String, String> {
        val config = Properties()
        config[ProducerConfig.CLIENT_ID_CONFIG] = "0"
        config[ProducerConfig.ACKS_CONFIG] = "all"
        config[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = brokerUrl
        config[ProducerConfig.CLIENT_ID_CONFIG] = applicationId

        // TODO add security

        return KafkaProducer(config, StringSerializer(), StringSerializer())
    }

    private fun aivenSecurityProps(properties: Properties) {
//        properties[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SSL"
//        properties[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] = getRequiredProperty(KAFKA_KEYSTORE_PATH)
//        properties[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = getRequiredProperty(KAFKA_CREDSTORE_PASSWORD)
//        properties[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = getRequiredProperty(KAFKA_TRUSTSTORE_PATH)
//        properties[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = getRequiredProperty(KAFKA_CREDSTORE_PASSWORD)
    }
}
