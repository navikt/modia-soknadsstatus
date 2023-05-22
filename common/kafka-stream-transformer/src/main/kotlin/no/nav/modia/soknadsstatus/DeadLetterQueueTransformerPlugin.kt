package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import no.nav.modia.soknadsstatus.kafka.*
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes.StringSerde
import javax.sql.DataSource

class DeadLetterTransformerConfig<DOMAIN_TYPE, TARGET_TYPE> {
    var appEnv: AppEnv? = null
    var filter: ((key: String, message: DOMAIN_TYPE) -> Boolean)? = null
    var transformer: ((key: String, message: DOMAIN_TYPE) -> TARGET_TYPE)? = null
    var targetSerde: Serde<TARGET_TYPE>? = null
    var domainSerde: Serde<DOMAIN_TYPE>? = null
    var skipTableDataSource: DataSource? = null
}

class DeadLetterQueueTransformerPlugin<DOMAIN_TYPE, TARGET_TYPE> :
    Plugin<Pipeline<*, ApplicationCall>, DeadLetterTransformerConfig<DOMAIN_TYPE, TARGET_TYPE>, DeadLetterQueueTransformerPlugin<DOMAIN_TYPE, TARGET_TYPE>> {
    private var producer: KafkaSoknadsstatusProducer<TARGET_TYPE>? = null
    private var filter: ((key: String, message: DOMAIN_TYPE) -> Boolean)? = null
    private var transformer: ((key: String, message: DOMAIN_TYPE) -> TARGET_TYPE)? = null
    private var domainSerde: Serde<DOMAIN_TYPE>? = null


    private val block: (suspend (topic: String, key: String, value: String) -> Result<Unit>) = { topic, key, value ->
        print("KEY $key")
        val domain = requireNotNull(domainSerde).deserializer().deserialize(topic, value.toByteArray(Charsets.UTF_8))

        if (filter == null) {
            transformAndSendMessage(key, domain)
        } else if (filter!!(key, domain)) {
            transformAndSendMessage(key, domain)
        } else {
            Result.success(Unit)
        }
    }

    private fun transformAndSendMessage(key: String, value: DOMAIN_TYPE): Result<Unit> {
        return try {
            val transformedValue = requireNotNull(transformer).invoke(key, value)
            requireNotNull(producer).sendMessage(key, transformedValue)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override val key: AttributeKey<DeadLetterQueueTransformerPlugin<DOMAIN_TYPE, TARGET_TYPE>> =
        AttributeKey("dead-letter-consumer")

    override fun install(
        pipeline: Pipeline<*, ApplicationCall>,
        configure: DeadLetterTransformerConfig<DOMAIN_TYPE, TARGET_TYPE>.() -> Unit
    ): DeadLetterQueueTransformerPlugin<DOMAIN_TYPE, TARGET_TYPE> {
        val configuration = DeadLetterTransformerConfig<DOMAIN_TYPE, TARGET_TYPE>()
        configuration.configure()

        val appEnv = requireNotNull(configuration.appEnv)

        transformer = configuration.transformer
        domainSerde = configuration.domainSerde
        producer = KafkaSoknadsstatusProducer(appEnv, requireNotNull(configuration.targetSerde))

        DeadLetterQueueConsumerPlugin().install(pipeline) {
            deadLetterQueueConsumer =
                DeadLetterQueueConsumerImpl(
                    topic = requireNotNull(appEnv.deadLetterQueueTopic),
                    kafkaConsumer = KafkaUtils.createConsumer(
                        appEnv,
                        StringSerde()
                    ),
                    block = block,
                    pollDurationMs = appEnv.deadLetterQueueConsumerPollIntervalMs,
                    deadLetterMessageSkipService = DeadLetterMessageSkipServiceImpl(
                        DeadLetterMessageRepository(
                            requireNotNull(appEnv.deadLetterQueueSkipTableName),
                            requireNotNull(configuration.skipTableDataSource)
                        )
                    ),
                    deadLetterQueueMetricsGauge = DeadLetterQueueMetricsGaugeImpl(requireNotNull(appEnv.deadLetterQueueMetricsGaugeName))
                )
        }

        return this
    }
}
