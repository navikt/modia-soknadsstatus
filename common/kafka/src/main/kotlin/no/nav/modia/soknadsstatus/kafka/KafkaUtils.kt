package no.nav.modia.soknadsstatus.kafka

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.errors.DeserializationExceptionHandler
import org.slf4j.LoggerFactory
import java.util.*

object KafkaUtils {
    private val log = LoggerFactory.getLogger("KafkaUtils")
    fun <T>createProducer(
        appConfig: AppEnv,
        valueSerde: Serde<T>
    ): KafkaProducer<String, T> {
        val props = Properties()
        commonProducerConfig(props, appConfig)

        return KafkaProducer(props, StringSerializer(), valueSerde.serializer())
    }

    fun <T> createConsumer(
        appConfig: AppEnv,
        valueSerde: Serde<T>,
    ): KafkaConsumer<String, T> {
        val props = Properties()
        commonConsumerConfig(props, appConfig)

        return KafkaConsumer(props, StringDeserializer(), valueSerde.deserializer())
    }

    fun <TARGET_TYPE> createStream(
        appConfig: AppEnv,
        valueSerde: Serde<TARGET_TYPE>,
        deserializationExceptionHandler: DeserializationExceptionHandler,
        deadLetterQueueProducer: DeadLetterQueueProducer?,
        configure: StreamsBuilder.() -> Unit,
    ): KafkaStreams {
        val props = Properties()
        commonStreamsConfig(
            props,
            appConfig,
            valueSerde,
            deserializationExceptionHandler,
            deadLetterQueueProducer
        )

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
