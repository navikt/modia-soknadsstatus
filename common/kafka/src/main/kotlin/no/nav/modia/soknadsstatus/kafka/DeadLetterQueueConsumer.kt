package no.nav.modia.soknadsstatus.kafka

import no.nav.personoversikt.common.logging.TjenestekallLogg
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords

class DeadLetterQueueConsumer(
    topic: String,
    private val deadLetterMessageSkipService: DeadLetterMessageSkipService,
    private val kafkaConsumer: Consumer<String, String>,
    pollDurationMs: Double,
    exceptionRestartDelayMs: Double,
    private val block: suspend (topic: String, key: String, value: String) -> Result<Unit>,
) : SoknadsstatusConsumerImpl(topic, kafkaConsumer, pollDurationMs, exceptionRestartDelayMs) {
    override suspend fun handleRecords(
        records: ConsumerRecords<String, String>,
        commitSync: () -> Unit,
    ) {
        if (records.count() > 0) {
            logger.info("Received number of DLQ records on topic $topic: ${records.count()}")
            for (record in records) {
                logger.info("Trying to process DL with key: ${record.key()}")
                val shouldSkipRecord = handlePossibleSkipRecord(record)
                if (shouldSkipRecord) continue

                val result = block(record.topic(), record.key(), record.value())
                if (result.isFailure) {
                    TjenestekallLogg.error(
                        "Klarte ikke å håndtere DL",
                        fields = mapOf("key" to record.key(), "value" to record.value()),
                    )
                    throw result.exceptionOrNull() ?: Exception().fillInStackTrace()
                }
            }
            logger.info("Committing offset ${kafkaConsumer.metrics()}")
        }
        commitSync()
    }

    private suspend fun handlePossibleSkipRecord(record: ConsumerRecord<String, String>): Boolean {
        if (record.key() == null) {
            TjenestekallLogg.info(
                "Skipping a dead letter with no key: ${record.value()}",
                fields = mapOf("record" to record.value()),
            )
            return true
        }
        if (deadLetterMessageSkipService.shouldSkip(record.key())) {
            TjenestekallLogg.info(
                "Skipping a dead letter due to key found in skip table: ${record.key()}",
                fields = mapOf("key" to record.key(), "value" to record.value()),
            )
            return true
        }
        return false
    }
}
