package no.nav.modia.soknadsstatus.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.Serde
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach


object TestUtils {
    abstract class WithKafka<KEY_TYPE, VALUE_TYPE>(private val keySerde: Serde<KEY_TYPE>, private val valueSerde: Serde<VALUE_TYPE>) {
        var producer: MockProducer<KEY_TYPE, VALUE_TYPE>? = null
        var consumer: MockConsumer<KEY_TYPE, VALUE_TYPE>? = null

        @BeforeEach
        fun setUp() {
            producer = MockProducer<KEY_TYPE, VALUE_TYPE>(
                true,
                keySerde.serializer(),
                valueSerde.serializer(),
            )
            consumer = MockConsumer<KEY_TYPE, VALUE_TYPE>(OffsetResetStrategy.EARLIEST)
        }

        fun startSubscribingToTopic(topic: String, records: Long, startSubscribing: () -> Unit){
            val partitions: MutableCollection<TopicPartition> = ArrayList()
            val partitionsBeginningMap: MutableMap<TopicPartition, Long> = HashMap()
            val partitionsEndMap: MutableMap<TopicPartition, Long> = HashMap()

            val partition = TopicPartition(topic, 1)

            partitions.add(partition)
            partitionsBeginningMap[partition] = 0L
            partitionsEndMap[partition] = records

            startSubscribing()

            consumer!!.rebalance(partitions)
            consumer!!.updateBeginningOffsets(partitionsBeginningMap)
            consumer!!.updateEndOffsets(partitionsEndMap)
        }

        @AfterEach
        fun tearDown() {
            producer = null
            consumer = null
        }
    }
}