package no.nav.modia.soknadsstatus.kafka

import kotlinx.coroutines.*
import no.nav.modia.soknadsstatus.BackgroundTask
import no.nav.modia.soknadsstatus.registerShutdownhook
import no.nav.personoversikt.common.logging.Logging.secureLog
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.errors.WakeupException
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

interface DeadLetterQueueConsumer {
    fun start()
}

class DeadLetterQueueConsumerImpl(
    private val topic: String,
    private val deadLetterMessageSkipService: DeadLetterMessageSkipService,
    private val kafkaConsumer: Consumer<String, String>,
    private val pollDurationMs: Double,
    private val deadLetterQueueMetricsGauge: DeadLetterQueueMetricsGauge,
    private val block: suspend (topic: String, key: String, value: String) -> Result<Unit>,
) : DeadLetterQueueConsumer {
    private val logger = LoggerFactory.getLogger("${DeadLetterQueueConsumerImpl::class.java.name}-$topic")

    private val closed = AtomicBoolean(false)
    private var job: Job? = null

    // Used to avoid infinite WakeupException loop. WakeUp exception is thrown by the Consumer the next time calling poll, if wakeUp has been called, but the consumer did not poll when calling wakeup.
    private val hasWakedUpConsumer = AtomicBoolean(false)

    init {
        registerShutdownhook {
            shutDown()
            kafkaConsumer.close()
        }
    }

    override fun start() {
        closed.set(false)
        kafkaConsumer.subscribe(listOf(topic))
        job = BackgroundTask.launch {
            pollAndProcessRecords()
        }
    }

    private suspend fun pollAndProcessRecords() {
        while (!closed.get()) {
            job?.ensureActive()
            try {
                val records = kafkaConsumer.poll(pollDurationMs.milliseconds.toJavaDuration())
                if (records.count() > 0) {
                    logger.info("Received number of DLQ records on topic $topic: ${records.count()}")
                    deadLetterQueueMetricsGauge.set(records.count())
                    for (record in records) {
                        logger.info("Trying to process DL with key: ${record.key()}")
                        if (record.key() == null) {
                            secureLog.info("Skipping a dead letter with no key: ${record.value()}")
                            deadLetterQueueMetricsGauge.decrement()
                            continue
                        }
                        if (deadLetterMessageSkipService.shouldSkip(record.key())) {
                            secureLog.info("Skipping a dead letter due to key found in skip table: ${record.key()}")
                            deadLetterQueueMetricsGauge.decrement()
                            continue
                        }
                        val result = block(record.topic(), record.key(), record.value())
                        if (result.isFailure) {
                            throw Exception("Failed to handle DLQ ${record.key()}: ${record.value()}")
                        } else {
                            deadLetterQueueMetricsGauge.decrement()
                        }
                    }
                    kafkaConsumer.commitSync()
                }
            } catch (e: Exception) {
                if (e is WakeupException && closed.get()) {
                    // Ignore exception if closing
                    return
                } else {
                    hasWakedUpConsumer.set(!(e is WakeupException && hasWakedUpConsumer.get()))
                }
                secureLog.error("Restarting DLQ consumer on topic $topic", e)
                restart()
                return
            }
        }
    }

    private fun restart() {
        kafkaConsumer.unsubscribe()
        shutDown()
        runBlocking {
            delay(pollDurationMs.milliseconds)
        }
        start()
    }

    private fun shutDown() {
        if (!hasWakedUpConsumer.get()) kafkaConsumer.wakeup()
        closed.set(true)
        job?.cancel()
    }
}
