package no.nav.modia.soknadsstatus.kafka

import no.nav.personoversikt.common.logging.Logging.secureLog
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.Serdes.StringSerde
import org.apache.kafka.streams.errors.DeserializationExceptionHandler
import org.apache.kafka.streams.processor.ProcessorContext
import java.util.UUID

class SendToDeadLetterQueueExceptionHandler : DeserializationExceptionHandler {
    private var dlqProducer: DeadLetterQueueProducer? = null

    override fun handle(
        context: ProcessorContext?,
        record: ConsumerRecord<ByteArray, ByteArray>?,
        exception: Exception?
    ): DeserializationExceptionHandler.DeserializationHandlerResponse {
        try {
            if (record?.value() == null) {
                return DeserializationExceptionHandler.DeserializationHandlerResponse.CONTINUE
            }

            val key: String

            if (record.key() == null) {
                key = UUID.randomUUID().toString()
                secureLog.error("Received a record without a key. The Dead letter was: ${record.value()}. Gave it key: $key")
            } else {
                key = StringSerde().deserializer().deserialize(record.topic(), record.key())
            }

            val data = StringSerde().deserializer().deserialize(record.topic(), record.value())
            requireNotNull(dlqProducer).sendMessage(key, data)
        } catch (e: Exception) {
            secureLog.error("Failed to parse message when sending to DLQ on topic ${record?.topic()}: ", e)
        }

        return DeserializationExceptionHandler.DeserializationHandlerResponse.CONTINUE
    }

    override fun configure(configs: MutableMap<String, *>?) {
        dlqProducer = configs?.get("dlqProducer") as DeadLetterQueueProducer?
    }
}
