package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import no.nav.modia.soknadsstatus.kafka.AppEnv
import no.nav.modia.soknadsstatus.kafka.ExceptionHandler
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.processor.api.*

class KafkaStreamTransformConfig<IN_TYPE, OUT_TYPE> {
    var appEnv: AppEnv? = null
    var sourceTopic: String? = null
    var targetTopic: String? = null
    var deserializer: ((key: String?, value: String) -> IN_TYPE)? = null
    var serializer: ((key: String?, value: OUT_TYPE) -> String?)? = null
    var deserializationExceptionHandler: ExceptionHandler? = null
    var onSerializationException: ((record: FixedKeyRecord<String, OUT_TYPE>, exception: Exception) -> Unit)? = null
    var configure: ((KStream<String, IN_TYPE>) -> KStream<String, OUT_TYPE>)? =
        null

    fun configure(fn: (KStream<String, IN_TYPE>) -> KStream<String, OUT_TYPE>) {
        this.configure = fn
    }
}

class KafkaStreamTransformPlugin<IN_TYPE, OUT_TYPE> :
    Plugin<Pipeline<*, PipelineCall>, KafkaStreamTransformConfig<IN_TYPE, OUT_TYPE>, KafkaStreamTransformPlugin<IN_TYPE, OUT_TYPE>> {
    override val key: AttributeKey<KafkaStreamTransformPlugin<IN_TYPE, OUT_TYPE>> =
        AttributeKey("kafka-stream-transform")

    override fun install(
        pipeline: Pipeline<*, PipelineCall>,
        configure: KafkaStreamTransformConfig<IN_TYPE, OUT_TYPE>.() -> Unit,
    ): KafkaStreamTransformPlugin<IN_TYPE, OUT_TYPE> {
        val configuration = KafkaStreamTransformConfig<IN_TYPE, OUT_TYPE>()
        configuration.configure()

        KafkaStreamPlugin().install(
            pipeline,
        ) {
            appEnv = requireNotNull(configuration.appEnv)
            topology {
                val stream = stream<String, String>(requireNotNull(configuration.sourceTopic))
                val deserializedStream: KStream<String, IN_TYPE> =
                    stream.processValues({
                        SerDesHandler<String, String, IN_TYPE>(
                            requireNotNull(configuration.deserializer),
                        ) { record, exception ->
                            requireNotNull(configuration.deserializationExceptionHandler).handle(
                                record.key(),
                                record.value(),
                                exception,
                            )
                        }
                    })
                val transformedStream = deserializedStream.let(requireNotNull(configuration.configure))
                val serializedStream: KStream<String, String> =
                    transformedStream.processValues({
                        SerDesHandler<String, OUT_TYPE, String>(
                            requireNotNull(configuration.serializer),
                            requireNotNull(configuration.onSerializationException),
                        )
                    })

                if (configuration.targetTopic != null) {
                    serializedStream.to(configuration.targetTopic)
                }
            }
        }

        return KafkaStreamTransformPlugin()
    }
}

private class SerDesHandler<KEY_TYPE : String?, VALUE_IN_TYPE, VALUE_OUT_TYPE>(
    private val block: (key: KEY_TYPE?, value: VALUE_IN_TYPE) -> VALUE_OUT_TYPE?,
    private val onException: (record: FixedKeyRecord<KEY_TYPE, VALUE_IN_TYPE>, exception: Exception) -> Unit,
) : FixedKeyProcessor<KEY_TYPE, VALUE_IN_TYPE, VALUE_OUT_TYPE> {
    private var context: FixedKeyProcessorContext<KEY_TYPE, VALUE_OUT_TYPE>? = null

    override fun init(context: FixedKeyProcessorContext<KEY_TYPE, VALUE_OUT_TYPE>?) {
        this.context = context
    }

    override fun process(record: FixedKeyRecord<KEY_TYPE, VALUE_IN_TYPE>?) {
        if (context == null) {
            throw IllegalStateException("Context was null")
        }
        if (record == null) {
            return
        }

        try {
            val newValue = record.withValue(block(record.key(), record.value()))
            context!!.forward(newValue)
        } catch (e: Exception) {
            onException(record, e)
        }
    }
}
