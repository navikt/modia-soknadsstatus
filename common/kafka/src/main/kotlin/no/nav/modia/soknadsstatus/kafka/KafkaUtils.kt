package no.nav.modia.soknadsstatus.kafka

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.slf4j.LoggerFactory
import java.util.*

object KafkaUtils {
    private val log = LoggerFactory.getLogger("KafkaUtils")
    fun createProducer(
        appConfig: AppEnv,
    ): KafkaProducer<String, String> {
        val props = Properties()
        commonProducerConfig(props, appConfig)

        return KafkaProducer(props, StringSerializer(), StringSerializer())
    }

    fun createConsumer(
        appConfig: AppEnv,
    ): KafkaConsumer<String, String> {
        val props = Properties()
        commonConsumerConfig(props, appConfig)

        return KafkaConsumer(props, StringDeserializer(), StringDeserializer())
    }

    fun createStream(
        appConfig: AppEnv,
        configure: StreamsBuilder.() -> Unit,
    ): KafkaStreams {
        val props = Properties()
        commonStreamsConfig(
            props,
            appConfig,
        )

        val builder = StreamsBuilder()
        builder.apply(configure)

        val topology = builder.build()
        log.info(
            """
            Created KStream: 
            ${topology.describe()}
            """.trimIndent(),
        )

        return KafkaStreams(topology, props)
    }
}
