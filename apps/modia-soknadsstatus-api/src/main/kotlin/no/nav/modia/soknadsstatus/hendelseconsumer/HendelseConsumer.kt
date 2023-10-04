package no.nav.modia.soknadsstatus.hendelseconsumer

import no.nav.modia.soknadsstatus.kafka.SendToDeadLetterQueueExceptionHandler
import no.nav.modia.soknadsstatus.kafka.SoknadsstatusConsumerImpl
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecords

class HendelseConsumer(
    private val sendToDeadLetterQueueExceptionHandler: SendToDeadLetterQueueExceptionHandler,
    topic: String,
    kafkaConsumer: Consumer<String, String>,
    pollDurationMs: Double,
    exceptionRestartDelayMs: Double,
    private val block: suspend (topic: String, key: String?, value: String) -> Result<Unit>,
) : SoknadsstatusConsumerImpl(topic, kafkaConsumer, pollDurationMs, exceptionRestartDelayMs) {
    override suspend fun handleRecords(
        records: ConsumerRecords<String, String>,
        commitSync: () -> Unit,
    ) {
        if (records.count() > 0) {
            logger.info("Mottok nye oppdatering på topic: $topic: ${records.count()}")
            for (record in records) {
                val result = block(record.topic(), record.key(), record.value())
                if (result.isFailure) {
                    sendToDeadLetterQueueExceptionHandler.handle(
                        record.key(),
                        record.value(),
                        result.exceptionOrNull() ?: Exception().fillInStackTrace(),
                    )
                }
            }
        }
        commitSync()
    }
}
