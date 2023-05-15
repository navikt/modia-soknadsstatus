package no.nav.modia.soknadsstatus.kafka

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.Serdes.StringSerde
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

const val TOPIC = "test-topic"
const val IGNORE_KEY = "ignore_key"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeadLetterQueueConsumerTest : TestUtils.WithKafka<String, String>(StringSerde(), StringSerde()) {
    private var deadLetterMessageSkipService = mockk<DeadLetterMessageSkipService>() {
        coEvery { shouldSkip(any()) } returns false
        coEvery { shouldSkip(IGNORE_KEY) } returns true
    }

    @Test
    fun `skal konsumere data`() {
        var message: String? = null


        val dlqConsumer = DeadLetterQueueConsumerImpl(
            topic = TOPIC,
            deadLetterMessageSkipService = deadLetterMessageSkipService,
            kafkaConsumer = consumer!!,
            pollDurationMs = 10.0,
        ) { _, _, value ->
            message = value
            Result.success(Unit)
        }

        startSubscribingToTopic(TOPIC, 1) {
            dlqConsumer.start()
        }


        val record = ConsumerRecord(
            TOPIC, 1, 0, "test_key", "test_message"
        )
        consumer!!.addRecord(record)


        runBlocking { delay(50L) }

        assertEquals("test_message", message)
    }

    @Test
    fun `skal ignorere meldinger uten nøkkel`() {
        val messages = mutableListOf<Pair<String, String>>()


        val dlqConsumer = DeadLetterQueueConsumerImpl(
            topic = TOPIC,
            deadLetterMessageSkipService = deadLetterMessageSkipService,
            kafkaConsumer = consumer!!,
            pollDurationMs = 10.0,
        ) { _, key, value ->
            messages.add(Pair(key, value))
            Result.success(Unit)
        }

        startSubscribingToTopic(TOPIC, 2) {
            dlqConsumer.start()
        }


        consumer!!.addRecord(
            ConsumerRecord(
                TOPIC, 1, 0, null, "ignore_message"
            )
        )

        consumer!!.addRecord(
            ConsumerRecord(
                TOPIC, 1, 1, "test_key", "test_message"
            )
        )


        runBlocking { delay(50L) }

        assertEquals("test_message", messages.first().second)
        assertEquals("test_key", messages.first().first)
    }

    @Test
    fun `skal ignorere meldinger når keyen ligger i skip tabellen`() {
        val messages = mutableListOf<Pair<String, String>>()

        val dlqConsumer = DeadLetterQueueConsumerImpl(
            topic = TOPIC,
            deadLetterMessageSkipService = deadLetterMessageSkipService,
            kafkaConsumer = consumer!!,
            pollDurationMs = 10.0,
        ) { _, key, value ->
            messages.add(Pair(key, value))
            Result.success(Unit)
        }

        startSubscribingToTopic(TOPIC, 2) {
            dlqConsumer.start()
        }


        consumer!!.addRecord(
            ConsumerRecord(
                TOPIC, 1, 0, IGNORE_KEY, "ignore_message"
            )
        )

        consumer!!.addRecord(
            ConsumerRecord(
                TOPIC, 1, 1, "test_key", "test_message"
            )
        )


        runBlocking { delay(50L) }

        assertEquals("test_message", messages.first().second)
        assertEquals("test_key", messages.first().first)
    }
}