package no.nav.modia.soknadsstatus.kafka

import no.nav.personoversikt.common.logging.TjenestekallLogg
import java.util.UUID

interface ExceptionHandler {
    fun handle(
        key: String?,
        value: String?,
        exception: Throwable,
    )
}

class SendToDeadLetterQueueExceptionHandler(
    private val topic: String,
    private val dlqProducer: DeadLetterQueueProducer,
) : ExceptionHandler {
    override fun handle(
        key: String?,
        value: String?,
        exception: Throwable,
    ) {
        TjenestekallLogg.error(
            "Producing dead letter $key: $value",
            mapOf("key" to key, "message" to value),
            throwable = exception,
        )
        try {
            if (value == null) {
                return
            }

            var dlqKey = key
            if (key == null) {
                dlqKey = UUID.randomUUID().toString()
                TjenestekallLogg.error(
                    "Received a record without a key. The Dead letter was: $value. Gave it key: $dlqKey",
                    fields = mapOf("value" to value, "key" to dlqKey),
                )
            }

            dlqProducer.sendMessage(dlqKey!!, value)
        } catch (e: Exception) {
            TjenestekallLogg.error(
                "Failed to parse message when sending to DLQ on topic $topic",
                fields = mapOf("topic" to topic),
                throwable = e,
            )
        }
    }
}
