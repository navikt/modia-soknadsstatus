package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import no.nav.modia.soknadsstatus.kafka.AppEnv
import no.nav.modia.soknadsstatus.kafka.DeadLetterQueueProducer
import no.nav.modia.soknadsstatus.kafka.SendToDeadLetterQueueExceptionHandler
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes.StringSerde
import org.apache.kafka.streams.errors.DeserializationExceptionHandler
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced

class KafkaStreamTransformConfig<SOURCE_TYPE, DOMAIN_TYPE, TARGET_TYPE> {
    var appEnv: AppEnv? = null
    var domainTypeserde: Serde<DOMAIN_TYPE>? = null
    var targetTypeSerde: Serde<TARGET_TYPE>? = null
    var configure: ((KStream<String, DOMAIN_TYPE>) -> KStream<String, TARGET_TYPE?>)? =
        null
    var deserializationExceptionHandler: SendToDeadLetterQueueExceptionHandler<SOURCE_TYPE>? = null
    var deadLetterQueueProducer: DeadLetterQueueProducer<SOURCE_TYPE>? = null
    var dlqSerde: Serde<SOURCE_TYPE>? = null

    fun configure(fn: (KStream<String, DOMAIN_TYPE>) -> KStream<String, TARGET_TYPE?>) {
        this.configure = fn
    }
}

class KafkaStreamTransformPlugin<SOURCE_TYPE, DOMAIN_TYPE, TARGET_TYPE> :
    Plugin<Pipeline<*, ApplicationCall>, KafkaStreamTransformConfig<SOURCE_TYPE, DOMAIN_TYPE, TARGET_TYPE>, KafkaStreamTransformPlugin<SOURCE_TYPE, DOMAIN_TYPE, TARGET_TYPE>> {
    override val key: AttributeKey<KafkaStreamTransformPlugin<SOURCE_TYPE, DOMAIN_TYPE, TARGET_TYPE>> = AttributeKey("kafka-stream-transform-v2")
    override fun install(
        pipeline: Pipeline<*, ApplicationCall>,
        configure: KafkaStreamTransformConfig<SOURCE_TYPE, DOMAIN_TYPE, TARGET_TYPE>.() -> Unit
    ): KafkaStreamTransformPlugin<SOURCE_TYPE, DOMAIN_TYPE, TARGET_TYPE> {
        val configuration = KafkaStreamTransformConfig<SOURCE_TYPE, DOMAIN_TYPE, TARGET_TYPE>()
        configuration.configure()

        KafkaStreamPlugin<SOURCE_TYPE, DOMAIN_TYPE>().install(
            pipeline
        ) {
            appEnv = requireNotNull(configuration.appEnv)
            valueSerde = requireNotNull(configuration.domainTypeserde)
            deserializationExceptionHandler = requireNotNull(configuration.deserializationExceptionHandler)
            deadLetterQueueProducer = configuration.deadLetterQueueProducer
            dlqSerde = requireNotNull(configuration.dlqSerde)
            topology {
                val targetTopic = configuration.appEnv?.targetTopic
                val stream = stream<String, DOMAIN_TYPE>(requireNotNull(configuration.appEnv?.sourceTopic))
                    .let(requireNotNull(configuration.configure))
                if (targetTopic != null) {
                    stream.to(targetTopic, Produced.with(StringSerde(), configuration.targetTypeSerde))
                }
            }
        }

        return KafkaStreamTransformPlugin()
    }
}
