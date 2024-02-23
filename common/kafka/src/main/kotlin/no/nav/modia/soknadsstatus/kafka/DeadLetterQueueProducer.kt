package no.nav.modia.soknadsstatus.kafka

import no.nav.personoversikt.common.logging.TjenestekallLogg
import org.apache.kafka.clients.producer.ProducerRecord

interface DeadLetterQueueProducer {
    fun sendMessage(
        topic: String,
        key: String,
        message: String,
    ): Result<Unit>
}

class DeadLetterQueueProducerImpl(
    private val appEnv: AppEnv,
) : DeadLetterQueueProducer {
    private val producer = KafkaUtils.createProducer(appEnv)

    override fun sendMessage(
        topic: String,
        key: String,
        message: String,
    ): Result<Unit> =
        try {
            val producerRecord =
                ProducerRecord(
                    topic,
                    key,
                    message,
                )
            producer.send(producerRecord)
            TjenestekallLogg.info("Produced dead letter $key: $message", mapOf("key" to key, "message" to message))
            Result.success(Unit)
        } catch (e: Exception) {
            TjenestekallLogg.error(
                "Failed to produce dead letter",
                fields = mapOf("key" to key, "message" to message),
                throwable = e,
            )
            Result.failure(e)
        }
}
