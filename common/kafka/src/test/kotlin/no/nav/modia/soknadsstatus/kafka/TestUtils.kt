package no.nav.modia.soknadsstatus.kafka

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.Serde
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

object TestUtils {
    abstract class WithKafka<KEY_TYPE, VALUE_TYPE>(
        private val keySerde: Serde<KEY_TYPE>,
        private val valueSerde: Serde<VALUE_TYPE>
    ) {
        private var partitionCounter = AtomicInteger(0)

        fun getExtendedMockConsumer(topic: String, numberOfRecords: Long): ExtendedMockConsumer<KEY_TYPE, VALUE_TYPE> {
            val partition = partitionCounter.getAndIncrement()

            return ExtendedMockConsumer(topic, partition, numberOfRecords)
        }

        class ExtendedMockConsumer<KEY_TYPE, VALUE_TYPE>(
            private val topic: String,
            private val partition: Int,
            private val numberOfRecords: Long
        ) :
            MockConsumer<KEY_TYPE, VALUE_TYPE>(OffsetResetStrategy.EARLIEST) {
            private val topicPartition = TopicPartition(topic, partition)
            private var offsetCounter = 0L
            val messages = mutableListOf<Pair<KEY_TYPE, VALUE_TYPE>>()

            fun startSubscribingToTopic(
                startSubscribing: () -> Unit
            ) {
                val partitions: MutableCollection<TopicPartition> = ArrayList()
                val partitionsBeginningMap: MutableMap<TopicPartition, Long> = HashMap()
                val partitionsEndMap: MutableMap<TopicPartition, Long> = HashMap()

                partitions.add(topicPartition)
                partitionsBeginningMap[topicPartition] = 0L
                partitionsEndMap[topicPartition] = numberOfRecords

                startSubscribing()

                this.rebalance(partitions)
                this.updateBeginningOffsets(partitionsBeginningMap)
                this.updateEndOffsets(partitionsEndMap)
            }

            fun addRecord(key: KEY_TYPE?, value: VALUE_TYPE) {
                val record = ConsumerRecord(
                    topic,
                    partition,
                    getAndIncrementOffset(),
                    key,
                    value
                )

                this.addRecord(record)
            }

            fun acknowledgeMessage(message: Pair<KEY_TYPE, VALUE_TYPE>) = messages.add(message)

            fun waitForMessages(
                expectedNumberOfMessages: Int,
                delayDuration: Duration = 500.milliseconds,
                retry: Int = 10
            ) {
                var counter = 0
                while (messages.size != expectedNumberOfMessages && counter < retry) {
                    runBlocking { delay(delayDuration) }
                    counter++
                    continue
                }
            }

            private fun getAndIncrementOffset(): Long {
                offsetCounter++
                return offsetCounter - 1
            }
        }
    }
}
