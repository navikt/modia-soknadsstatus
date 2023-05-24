package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import no.nav.modia.soknadsstatus.kafka.AppEnv
import no.nav.modia.soknadsstatus.kafka.DeadLetterQueueProducer
import no.nav.modia.soknadsstatus.kafka.SendToDeadLetterQueueExceptionHandler
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes.StringSerde
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced

class KafkaStreamTransformConfig<DOMAIN_TYPE, TARGET_TYPE> {
    var appEnv: AppEnv? = null
    var domainSerde: Serde<DOMAIN_TYPE>? = null
    var targetSerde: Serde<TARGET_TYPE>? = null
    var deserializationExceptionHandler: SendToDeadLetterQueueExceptionHandler? = null
    var deadLetterQueueProducer: DeadLetterQueueProducer? = null
    var configure: ((KStream<String, DOMAIN_TYPE>) -> KStream<String, TARGET_TYPE?>)? =
        null


    fun configure(fn: (KStream<String, DOMAIN_TYPE>) -> KStream<String, TARGET_TYPE?>) {
        this.configure = fn
    }
}

class KafkaStreamTransformPlugin<DOMAIN_TYPE, TARGET_TYPE> :
    Plugin<Pipeline<*, ApplicationCall>, KafkaStreamTransformConfig<DOMAIN_TYPE, TARGET_TYPE>, KafkaStreamTransformPlugin<DOMAIN_TYPE, TARGET_TYPE>> {
    override val key: AttributeKey<KafkaStreamTransformPlugin<DOMAIN_TYPE, TARGET_TYPE>> = AttributeKey("kafka-stream-transform")
    override fun install(
        pipeline: Pipeline<*, ApplicationCall>,
        configure: KafkaStreamTransformConfig<DOMAIN_TYPE, TARGET_TYPE>.() -> Unit
    ): KafkaStreamTransformPlugin<DOMAIN_TYPE, TARGET_TYPE> {
        val configuration = KafkaStreamTransformConfig<DOMAIN_TYPE, TARGET_TYPE>()
        configuration.configure()

        KafkaStreamPlugin<DOMAIN_TYPE>().install(
            pipeline
        ) {
            appEnv = requireNotNull(configuration.appEnv)
            valueSerde = requireNotNull(configuration.domainSerde)
            deserializationExceptionHandler = requireNotNull(configuration.deserializationExceptionHandler)
            deadLetterQueueProducer = configuration.deadLetterQueueProducer
            topology {
                val targetTopic = configuration.appEnv?.targetTopic
                val stream = stream<String, DOMAIN_TYPE>(requireNotNull(configuration.appEnv?.sourceTopic))
                    .let(requireNotNull(configuration.configure))
                if (targetTopic != null) {
                    stream.to(targetTopic, Produced.with(StringSerde(), configuration.targetSerde))
                }
            }
        }

        return KafkaStreamTransformPlugin()
    }
}
