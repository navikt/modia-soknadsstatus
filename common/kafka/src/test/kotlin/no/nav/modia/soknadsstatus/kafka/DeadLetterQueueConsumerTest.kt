package no.nav.modia.soknadsstatus.kafka

import io.mockk.coEvery
import io.mockk.mockk
import org.apache.kafka.common.serialization.Serdes.StringSerde
import org.junit.Ignore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

const val TOPIC = "test-topic"
const val IGNORE_KEY = "ignore_key"

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@Timeout(10, unit = TimeUnit.SECONDS)
class DeadLetterQueueConsumerTest : TestUtils.WithKafka<String, String>(StringSerde(), StringSerde()) {
    private var deadLetterMessageSkipService =
        mockk<DeadLetterMessageSkipService> {
            coEvery { shouldSkip(any()) } returns false
            coEvery { shouldSkip(IGNORE_KEY) } returns true
        }

    @Test
    fun `skal konsumere data`() {
        val consumer = getExtendedMockConsumer(TOPIC, 1)

        val dlqConsumer =
            DeadLetterQueueConsumer(
                topic = TOPIC,
                deadLetterMessageSkipService = deadLetterMessageSkipService,
                kafkaConsumer = consumer,
                pollDurationMs = 100.0,
                exceptionRestartDelayMs = 1000.0,
                deadLetterQueueMetricsGauge = DeadLetterQueueMetricsGaugeImpl("TestGauge"),
            ) { _, key, value ->
                consumer.acknowledgeMessage(Pair(key, value))
                Result.success(Unit)
            }

        consumer.startSubscribingToTopic {
            dlqConsumer.startAndWait()
        }

        consumer.addRecord("test_key", "test_message")

        dlqConsumer.startAndWait()

        consumer.waitForMessages(1)

        dlqConsumer.shutDownAndWait()

        assertEquals("test_key", consumer.messages.first().first)
        assertEquals("test_message", consumer.messages.first().second)
    }

    @Ignore
    fun `skal ignorere meldinger uten nøkkel`() {
        val consumer = getExtendedMockConsumer(TOPIC, 2)

        val dlqConsumer =
            DeadLetterQueueConsumer(
                topic = TOPIC,
                deadLetterMessageSkipService = deadLetterMessageSkipService,
                kafkaConsumer = consumer,
                pollDurationMs = 100.0,
                exceptionRestartDelayMs = 1000.0,
                deadLetterQueueMetricsGauge = DeadLetterQueueMetricsGaugeImpl("TestGauge"),
            ) { _, key, value ->
                consumer.acknowledgeMessage(Pair(key, value))
                Result.success(Unit)
            }

        consumer.startSubscribingToTopic {
            dlqConsumer.startAndWait()
        }

        consumer.addRecord(
            null,
            "ignore_message",
        )

        consumer.addRecord(
            "test_key",
            "test_message",
        )

        consumer.waitForMessages(1)

        dlqConsumer.shutDownAndWait()

        assertEquals("test_message", consumer.messages.first().second)
        assertEquals("test_key", consumer.messages.first().first)
    }

    @Ignore
    fun `skal ignorere meldinger når keyen ligger i skip tabellen`() {
        val consumer = getExtendedMockConsumer(TOPIC, 2)

        val dlqConsumer =
            DeadLetterQueueConsumer(
                topic = TOPIC,
                deadLetterMessageSkipService = deadLetterMessageSkipService,
                kafkaConsumer = consumer,
                pollDurationMs = 100.0,
                exceptionRestartDelayMs = 1000.0,
                deadLetterQueueMetricsGauge = DeadLetterQueueMetricsGaugeImpl("TestGauge"),
            ) { _, key, value ->
                consumer.acknowledgeMessage(Pair(key, value))
                Result.success(Unit)
            }

        consumer.startSubscribingToTopic { dlqConsumer.startAndWait() }

        consumer.addRecord(
            IGNORE_KEY,
            "ignore_message",
        )

        consumer.addRecord(
            "test_key",
            "test_message",
        )

        consumer.waitForMessages(1)

        dlqConsumer.shutDownAndWait()

        assertEquals("test_message", consumer.messages.first().second)
        assertEquals("test_key", consumer.messages.first().first)
    }
}
