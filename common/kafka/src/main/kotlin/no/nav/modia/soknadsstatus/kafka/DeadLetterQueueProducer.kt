package no.nav.modia.soknadsstatus.kafka

import no.nav.personoversikt.common.logging.TjenestekallLogg
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serdes.StringSerde

class DeadLetterQueueProducer(
    private val appEnv: AppEnv,
) : SoknadsstatusProducer<String> {
    private val producer = KafkaUtils.createProducer(appEnv, StringSerde())

    override fun sendMessage(key: String, message: String): Result<Unit> {
        return try {
            val producerRecord = ProducerRecord(appEnv.deadLetterQueueTopic, key, message)
            producer.send(producerRecord)
            TjenestekallLogg.info("Produced dead letter $key: $message", mapOf("key" to key, "message" to message))
            Result.success(Unit)
        } catch (e: Exception) {
            TjenestekallLogg.error("Failed to produce dead letter", fields = mapOf("key" to key, "message" to message), throwable = e)
            Result.failure(e)
        }
    }
}
