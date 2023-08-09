package no.nav.modia.soknadsstatus.kafka

import kotlinx.coroutines.*
import no.nav.modia.soknadsstatus.BackgroundTask
import no.nav.modia.soknadsstatus.registerShutdownhook
import no.nav.personoversikt.common.logging.TjenestekallLogg
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.errors.WakeupException
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

interface DeadLetterQueueConsumer {
    fun start()
    fun waitForStart()
    fun waitForShutdown()
    fun shutDownAndWait()
    fun startAndWait()
}

class DeadLetterQueueConsumerImpl(
    private val topic: String,
    private val deadLetterMessageSkipService: DeadLetterMessageSkipService,
    private val kafkaConsumer: Consumer<String, String>,
    private val pollDurationMs: Double,
    private val exceptionRestartDelayMs: Double,
    private val deadLetterQueueMetricsGauge: DeadLetterQueueMetricsGauge,
    private val block: suspend (topic: String, key: String, value: String) -> Result<Unit>,
) : DeadLetterQueueConsumer {
    private val logger = LoggerFactory.getLogger("${DeadLetterQueueConsumerImpl::class.java.name}-$topic")

    private val closed = AtomicBoolean(false)
    private val isRunning = AtomicBoolean(false)

    private var job: Job? = null

    // Used to avoid infinite WakeupException loop. WakeUp exception is thrown by the Consumer the next time calling poll, if wakeUp has been called, but the consumer did not poll when calling wakeup.
    private val hasWakedUpConsumer = AtomicBoolean(false)

    init {
        checkThatPollDurationIsLessThanExceptionDelay(
            pollDurationMs.milliseconds,
            exceptionRestartDelayMs.milliseconds
        )
        registerShutdownhook {
            runBlocking {
                shutDown()
            }
        }
    }

    override fun start() {
        job = BackgroundTask.launch {
            startConsumer()
        }
    }

    private suspend fun startConsumer() {
        closed.set(false)
        kafkaConsumer.subscribe(listOf(topic))
        pollAndProcessRecords()
    }

    private suspend fun pollAndProcessRecords() {
        logger.info("Starting to poll and process records from $topic")
        outer@ while (!closed.get()) {
            job?.ensureActive()
            isRunning.set(true)
            try {
                val records = kafkaConsumer.poll(pollDurationMs.milliseconds.toJavaDuration())
                if (records.count() > 0) {
                    logger.info("Received number of DLQ records on topic $topic: ${records.count()}")
                    for (record in records) {
                        logger.info("Trying to process DL with key: ${record.key()}")
                        val shouldSkipRecord = handlePossibleSkipRecord(record)
                        if (shouldSkipRecord) continue

                        val result = block(record.topic(), record.key(), record.value())
                        if (result.isFailure) {
                            TjenestekallLogg.error(
                                "Failed to handle DLQ ${record.key()}",
                                fields = mapOf("key" to record.key(), "value" to record.value()),
                                throwable = Exception().fillInStackTrace()
                            )
                            throw IllegalArgumentException("Failed to parse DLQ record")
                        } else {
                            deadLetterQueueMetricsGauge.decrement()
                        }
                    }
                    logger.info("Committing offset ${kafkaConsumer.metrics()}")
                    kafkaConsumer.commitSync()
                }
            } catch (e: Exception) {
                isRunning.set(false)
                if (consumerExceptionShouldBeIgnored(e)) {
                    return
                } else {
                    hasWakedUpConsumer.set(!(e is WakeupException && hasWakedUpConsumer.get()))
                }
                TjenestekallLogg.error(
                    "Restarting DLQ consumer on topic $topic",
                    fields = mapOf("topic" to topic),
                    throwable = e
                )
                restart()
                continue@outer
            }
        }
    }

    private suspend fun restart() {
        stopConsumer()
        logger.info("Delaying restart of DLQ consumer with $exceptionRestartDelayMs")
        delay(exceptionRestartDelayMs.toLong())
        startConsumer()
    }

    private fun stopConsumer() {
        logger.info("Shutting down DLQ consumer on topic: $topic")
        kafkaConsumer.unsubscribe()
        if (!hasWakedUpConsumer.get()) kafkaConsumer.wakeup()
        closed.set(true)
    }

    suspend fun shutDown() {
        stopConsumer()
        kafkaConsumer.close()
        job?.cancelAndJoin()
    }

    private suspend fun handlePossibleSkipRecord(record: ConsumerRecord<String, String>): Boolean {
        if (record.key() == null) {
            TjenestekallLogg.info(
                "Skipping a dead letter with no key: ${record.value()}",
                fields = mapOf("record" to record.value())
            )
            deadLetterQueueMetricsGauge.decrement()
            return true
        }
        if (deadLetterMessageSkipService.shouldSkip(record.key())) {
            TjenestekallLogg.info(
                "Skipping a dead letter due to key found in skip table: ${record.key()}",
                fields = mapOf("key" to record.key(), "value" to record.value())
            )
            deadLetterQueueMetricsGauge.decrement()
            return true
        }
        return false
    }

    private fun consumerExceptionShouldBeIgnored(e: Exception) = e is WakeupException && closed.get()

    private fun checkThatPollDurationIsLessThanExceptionDelay(
        pollDuration: Duration,
        delayDuration: Duration
    ): Boolean {
        if (delayDuration <= pollDuration) {
            throw IllegalArgumentException("Delay duration must be larger than the poll duration. Otherwise the commit will commit wrong offset.")
        }
        return true
    }

    override fun waitForStart() {
        while (!isRunning.get()) {
            continue
        }
    }

    override fun waitForShutdown() {
        while (isRunning.get()) {
            continue
        }
    }

    override fun startAndWait() {
        start()
        waitForStart()
    }

    override fun shutDownAndWait() {
        runCatching {
            runBlocking {
                shutDown()
            }
        }
        waitForShutdown()
    }
}
