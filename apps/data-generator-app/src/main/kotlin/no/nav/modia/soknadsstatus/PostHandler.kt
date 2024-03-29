package no.nav.modia.soknadsstatus

import no.nav.modia.soknadsstatus.jms.Jms
import no.nav.modia.soknadsstatus.jms.JmsProducer
import no.nav.modia.soknadsstatus.kafka.AppEnv
import no.nav.modia.soknadsstatus.kafka.KafkaUtils
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.UUID

fun interface PostHandler {
    operator fun invoke(
        source: Source,
        data: String,
    )
}

class JmsHandler(
    config: Jms.Config,
    appMode: AppMode,
) : PostHandler {
    private val producer = JmsProducer(config, appMode)

    override fun invoke(
        source: Source,
        data: String,
    ) {
        check(source.type == Source.Type.JMS)
        producer.send(source.resourceId, data)
    }
}

class KafkaHandler(
    appEnv: AppEnv,
) : PostHandler {
    private val producer = KafkaUtils.createProducer(appEnv)

    override fun invoke(
        source: Source,
        data: String,
    ) {
        check(source.type == Source.Type.KAFKA)
        producer.send(ProducerRecord(source.resourceId, UUID.randomUUID().toString(), data))
    }
}
