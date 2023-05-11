package no.nav.modia.soknadsstatus.kafka

import no.nav.personoversikt.common.logging.Logging
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serde

class DeadLetterQueueProducer<VALUE_TYPE>(
    private val appEnv: AppEnv,
    valueSerde: Serde<VALUE_TYPE>
) : SoknadsstatusProducer<VALUE_TYPE> {
    private val producer = KafkaUtils.createProducer(appEnv, valueSerde)

    override fun sendMessage(key: String, message: VALUE_TYPE): Result<Unit> {
        return try {
            val producerRecord = ProducerRecord(appEnv.deadLetterQueueTopic, key, message)
            producer.send(producerRecord)
            Logging.secureLog.info("Produced dead letter $key: $message")
            Result.success(Unit)
        } catch (e: Exception) {
            Logging.secureLog.error("Failed to produce dead letter", e)
            Result.failure(e)
        }
    }
}
