package no.nav.modia.soknadsstatus.kafka

import no.nav.personoversikt.common.logging.TjenestekallLogg
import org.apache.kafka.clients.producer.ProducerRecord

interface SoknadsstatusProducer {
    fun sendMessage(key: String, message: String): Result<Unit>
}

class KafkaSoknadsstatusProducer(
    private val appEnv: AppEnv,
) :

    SoknadsstatusProducer {
    private val producer = KafkaUtils.createProducer(appEnv)

    override fun sendMessage(key: String, message: String): Result<Unit> {
        return try {
            val producerRecord = ProducerRecord(appEnv.targetTopic, key, message)
            producer.send(producerRecord)
            Result.success(Unit)
        } catch (e: Exception) {
            TjenestekallLogg.error(
                "Failed to produce kafka message,",
                fields = mapOf("key" to key, "message" to message),
                throwable = e
            )
            Result.failure(e)
        }
    }
}
