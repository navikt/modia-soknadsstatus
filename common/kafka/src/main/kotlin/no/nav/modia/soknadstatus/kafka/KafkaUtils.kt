package no.nav.modia.soknadstatus.kafka

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.errors.DefaultProductionExceptionHandler
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler
import org.slf4j.LoggerFactory
import java.util.*

object KafkaUtils {
    private val log = LoggerFactory.getLogger("KafkaUtils")
    fun createProducer(
        applicationId: String,
        brokerUrl: String,
    ): KafkaProducer<String, String> {
        val config = Properties()
        config[ProducerConfig.ACKS_CONFIG] = "all"
        config[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = brokerUrl
        config[ProducerConfig.CLIENT_ID_CONFIG] = applicationId

        // TODO add security

        return KafkaProducer(config, StringSerializer(), StringSerializer())
    }

    fun createStream(
        applicationId: String,
        brokerUrl: String,
        configure: StreamsBuilder.() -> Unit
    ): KafkaStreams {
        val props = Properties()
        props[StreamsConfig.APPLICATION_ID_CONFIG] = applicationId
        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = brokerUrl
        props[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.StringSerde().javaClass
        props[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.StringSerde().javaClass
        props[StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG] = LogAndFailExceptionHandler::class.java
        props[StreamsConfig.DEFAULT_PRODUCTION_EXCEPTION_HANDLER_CLASS_CONFIG] = DefaultProductionExceptionHandler::class.java

        val builder = StreamsBuilder()
        builder.apply(configure)

        val topology = builder.build()
        log.info(
            """
            Created KStream: 
            ${topology.describe()}
            """.trimIndent()
        )

        return KafkaStreams(topology, props)
    }

    private fun aivenSecurityProps(properties: Properties) {
//        properties[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SSL"
//        properties[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] = getRequiredProperty(KAFKA_KEYSTORE_PATH)
//        properties[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = getRequiredProperty(KAFKA_CREDSTORE_PASSWORD)
//        properties[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = getRequiredProperty(KAFKA_TRUSTSTORE_PATH)
//        properties[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = getRequiredProperty(KAFKA_CREDSTORE_PASSWORD)
    }
}
