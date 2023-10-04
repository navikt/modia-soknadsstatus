package no.nav.modia.soknadsstatus.kafka

import kotlinx.coroutines.*
import no.nav.modia.soknadsstatus.BackgroundTask
import no.nav.modia.soknadsstatus.registerShutdownhook
import no.nav.personoversikt.common.logging.TjenestekallLogg
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.errors.WakeupException
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

interface SoknadsstatusConsumer {
    fun start()

    fun waitForStart()

    fun waitForShutdown()

    fun shutDownAndWait()

    fun startAndWait()
}

open class SoknadsstatusConsumerImpl(
    protected val topic: String,
    private val kafkaConsumer: Consumer<String, String>,
    private val pollDurationMs: Double,
    private val exceptionRestartDelayMs: Double,
) : SoknadsstatusConsumer {
    protected val logger = LoggerFactory.getLogger("${SoknadsstatusConsumerImpl::class.java.name}-$topic")

    private val closed = AtomicBoolean(false)
    private val isRunning = AtomicBoolean(false)

    private var job: Job? = null

    open suspend fun handleRecords(
        records: ConsumerRecords<String, String>,
        commitSync: () -> Unit,
    ): Unit = throw NotImplementedError("handleRecords må implementeres av arvende klasse")

    // Used to avoid infinite WakeupException loop. WakeUp exception is thrown by the Consumer the next time calling poll, if wakeUp has been called, but the consumer did not poll when calling wakeup.
    private val hasWakedUpConsumer = AtomicBoolean(false)

    init {
        checkThatPollDurationIsLessThanExceptionDelay(
            pollDurationMs.milliseconds,
            exceptionRestartDelayMs.milliseconds,
        )
        registerShutdownhook {
            runBlocking {
                shutDown()
            }
        }
    }

    override fun start() {
        job =
            BackgroundTask.launch {
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
                handleRecords(records) { kafkaConsumer.commitSync() }
            } catch (e: Exception) {
                isRunning.set(false)
                if (consumerExceptionShouldBeIgnored(e)) {
                    return
                } else {
                    hasWakedUpConsumer.set(!(e is WakeupException && hasWakedUpConsumer.get()))
                }
                TjenestekallLogg.error(
                    "Restarting consumer on topic $topic",
                    fields = mapOf("topic" to topic),
                    throwable = e,
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

    private fun consumerExceptionShouldBeIgnored(e: Exception) = e is WakeupException && closed.get()

    private fun checkThatPollDurationIsLessThanExceptionDelay(
        pollDuration: Duration,
        delayDuration: Duration,
    ): Boolean {
        if (delayDuration <= pollDuration) {
            throw IllegalArgumentException(
                "Delay duration must be larger than the poll duration. Otherwise the commit will commit wrong offset.",
            )
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
