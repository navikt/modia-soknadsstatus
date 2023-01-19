package no.nav.modia.soknadstatus

import no.nav.modia.soknadstatus.jms.Jms
import no.nav.modia.soknadstatus.jms.JmsProducer
import no.nav.modia.soknadstatus.kafka.KafkaUtils
import org.apache.kafka.clients.producer.ProducerRecord

fun interface PostHandler {
    operator fun invoke(source: Source, data: String)
}

class JmsHandler(config: Jms.Config) : PostHandler {
    private val producer = JmsProducer(config)
    override fun invoke(source: Source, data: String) {
        check(source.type == Source.Type.JMS)
        producer.send(source.resourceId, data)
    }
}

class KafkaHandler(brokerId: String) : PostHandler {
    private val producer = KafkaUtils.createProducer("data-generator-app", brokerId)
    override fun invoke(source: Source, data: String) {
        check(source.type == Source.Type.KAFKA)
        producer.send(ProducerRecord(source.resourceId, data))
    }
}
