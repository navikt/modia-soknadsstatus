package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import no.nav.modia.soknadsstatus.kafka.*
import javax.sql.DataSource

class DeadLetterTransformerConfig<INTERNAL_TYPE, OUT_TYPE> {
    var appEnv: AppEnv? = null
    var filter: ((key: String, message: INTERNAL_TYPE) -> Boolean)? = null
    var deserializer: ((key: String?, stringValue: String) -> INTERNAL_TYPE)? = null
    var transformer: ((key: String, message: INTERNAL_TYPE) -> OUT_TYPE)? = null
    var serializer: ((key: String?, value: OUT_TYPE) -> String)? = null
    var skipTableDataSource: DataSource? = null
    var deadLetterQueueMetricsGauge: DeadLetterQueueMetricsGauge? = null
}

class DeadLetterQueueTransformerPlugin<INTERNAL_TYPE, OUT_TYPE> :
    Plugin<Pipeline<*, ApplicationCall>, DeadLetterTransformerConfig<INTERNAL_TYPE, OUT_TYPE>, DeadLetterQueueTransformerPlugin<INTERNAL_TYPE, OUT_TYPE>> {
    private lateinit var producer: KafkaSoknadsstatusProducer
    private var filter: ((key: String, message: INTERNAL_TYPE) -> Boolean)? = null
    private lateinit var transformer: ((key: String, message: INTERNAL_TYPE) -> OUT_TYPE)
    private lateinit var deserializer: ((key: String?, stringValue: String) -> INTERNAL_TYPE)
    private lateinit var serializer: (key: String?, value: OUT_TYPE) -> String

    private val block: (suspend (topic: String, key: String, value: String) -> Result<Unit>) =
        { _, key, value ->
            runCatching {
                val domainValue = deserializer(key, value)
                if (filter == null || filter!!(key, domainValue)) {
                    transformAndSendMessage(key, domainValue)
                } else {
                    Unit
                }
            }
        }

    private fun transformAndSendMessage(key: String, value: INTERNAL_TYPE): Result<Unit> {
        val transformedValue = transformer.invoke(key, value)
        val serializedValue = serializer(key, transformedValue)
        return producer.sendMessage(key, serializedValue)
    }

    override val key: AttributeKey<DeadLetterQueueTransformerPlugin<INTERNAL_TYPE, OUT_TYPE>> =
        AttributeKey("dead-letter-consumer")

    override fun install(
        pipeline: Pipeline<*, ApplicationCall>,
        configure: DeadLetterTransformerConfig<INTERNAL_TYPE, OUT_TYPE>.() -> Unit,
    ): DeadLetterQueueTransformerPlugin<INTERNAL_TYPE, OUT_TYPE> {
        val configuration = DeadLetterTransformerConfig<INTERNAL_TYPE, OUT_TYPE>()
        configuration.configure()

        val appEnv = requireNotNull(configuration.appEnv)

        transformer = requireNotNull(configuration.transformer)
        deserializer = requireNotNull(configuration.deserializer)
        filter = configuration.filter
        serializer = requireNotNull(configuration.serializer)

        producer = KafkaSoknadsstatusProducer(
            appEnv,
        )

        DeadLetterQueueConsumerPlugin().install(pipeline) {
            deadLetterQueueConsumer =
                DeadLetterQueueConsumerImpl(
                    topic = requireNotNull(appEnv.deadLetterQueueTopic),
                    kafkaConsumer = KafkaUtils.createConsumer(
                        appEnv,
                    ),
                    block = block,
                    pollDurationMs = appEnv.deadLetterQueueConsumerPollIntervalMs,
                    exceptionRestartDelayMs = appEnv.deadLetterQueueExceptionRestartDelayMs,
                    deadLetterMessageSkipService = DeadLetterMessageSkipServiceImpl(
                        DeadLetterMessageRepository(
                            requireNotNull(appEnv.deadLetterQueueSkipTableName),
                            requireNotNull(configuration.skipTableDataSource),
                        ),
                    ),
                    deadLetterQueueMetricsGauge = requireNotNull(configuration.deadLetterQueueMetricsGauge),
                )
        }

        return this
    }
}
