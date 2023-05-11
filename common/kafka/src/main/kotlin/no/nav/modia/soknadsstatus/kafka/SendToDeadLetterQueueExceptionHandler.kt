package no.nav.modia.soknadsstatus.kafka

import no.nav.personoversikt.common.logging.Logging
import no.nav.personoversikt.common.logging.Logging.secureLog
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes.StringSerde
import org.apache.kafka.streams.errors.DeserializationExceptionHandler
import org.apache.kafka.streams.processor.ProcessorContext

class SendToDeadLetterQueueExceptionHandler<VALUE_TYPE> : DeserializationExceptionHandler {
    private var dlqProducer: DeadLetterQueueProducer<VALUE_TYPE>? = null
    private var dlqSerde: Serde<VALUE_TYPE>? = null

    override fun handle(
        context: ProcessorContext?,
        record: ConsumerRecord<ByteArray, ByteArray>?,
        exception: Exception?
    ): DeserializationExceptionHandler.DeserializationHandlerResponse {
        try {
            if (record?.value() == null) {
                return DeserializationExceptionHandler.DeserializationHandlerResponse.CONTINUE
            }
            if (record.key() == null) {
                secureLog.error("Can not handle DLQ without a key. The Dead letter was: ${record.value()}")
            }
            val data = requireNotNull(dlqSerde).deserializer().deserialize(record.topic(), record.value())
            val key = StringSerde().deserializer().deserialize(record.topic(), record.key())
            requireNotNull(dlqProducer).sendMessage(key, data)
        } catch (e: Exception) {
            secureLog.error("Failed to parse message when sending to DLQ on topic ${record?.topic()}: ", e)
        }

        return DeserializationExceptionHandler.DeserializationHandlerResponse.CONTINUE
    }

    override fun configure(configs: MutableMap<String, *>?) {
        dlqProducer = configs?.get("dlqProducer") as DeadLetterQueueProducer<VALUE_TYPE>?
        dlqSerde = configs?.get("dlqSerde") as Serde<VALUE_TYPE>?
    }
}
