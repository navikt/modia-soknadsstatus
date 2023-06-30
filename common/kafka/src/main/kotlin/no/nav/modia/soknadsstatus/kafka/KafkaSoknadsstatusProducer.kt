package no.nav.modia.soknadsstatus.kafka

import no.nav.personoversikt.common.logging.Logging
import no.nav.personoversikt.common.logging.TjenestekallLogg
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serde

interface SoknadsstatusProducer<VALUE_TYPE> {
    fun sendMessage(key: String, message: VALUE_TYPE): Result<Unit>
}

class KafkaSoknadsstatusProducer<VALUE_TYPE>(private val appEnv: AppEnv, valueSerde: Serde<VALUE_TYPE>) :
    SoknadsstatusProducer<VALUE_TYPE> {
    private val producer = KafkaUtils.createProducer(appEnv, valueSerde)

    override fun sendMessage(key: String, message: VALUE_TYPE): Result<Unit> {
        return try {
            val producerRecord = ProducerRecord(appEnv.targetTopic, key, message)
            producer.send(producerRecord)
            Result.success(Unit)
        } catch (e: Exception) {
            TjenestekallLogg.error("Failed to produce kafka message,", fields = mapOf("key" to key, "message" to message), throwable = e)
            Result.failure(e)
        }
    }
}
