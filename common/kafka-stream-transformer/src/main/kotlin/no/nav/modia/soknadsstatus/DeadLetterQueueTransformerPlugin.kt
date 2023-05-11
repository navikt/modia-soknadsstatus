package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import no.nav.modia.soknadsstatus.kafka.*
import org.apache.kafka.common.serialization.Serde
import javax.sql.DataSource

class DeadLetterTransformerConfig<SOURCE_VALUE, TARGET_VALUE> {
    var appEnv: AppEnv? = null
    var sourceSerde: Serde<SOURCE_VALUE>? = null
    var filter: ((key: String, message: SOURCE_VALUE) -> Boolean)? = null
    var transformer: ((key: String, message: SOURCE_VALUE) -> TARGET_VALUE)? = null
    var targetSerde: Serde<TARGET_VALUE>? = null
    var skipTableDataSource: DataSource? = null
}

class DeadLetterQueueTransformerPlugin<SOURCE_VALUE, TARGET_VALUE> :
    Plugin<Pipeline<*, ApplicationCall>, DeadLetterTransformerConfig<SOURCE_VALUE, TARGET_VALUE>, DeadLetterQueueTransformerPlugin<SOURCE_VALUE, TARGET_VALUE>> {
    private var producer: KafkaSoknadsstatusProducer<TARGET_VALUE>? = null
    private var transformer: ((key: String, message: SOURCE_VALUE) -> TARGET_VALUE)? = null
    private var filter: ((key: String, message: SOURCE_VALUE) -> Boolean)? = null

    private val block: (suspend (key: String, value: SOURCE_VALUE) -> Result<Unit>) = { key, value ->
        if (filter == null) {
            transformAndSendMessage(key, value)
        } else if (filter!!(key, value)) {
            transformAndSendMessage(key, value)
        } else {
            Result.success(Unit)
        }
    }

    private fun transformAndSendMessage(key: String, value: SOURCE_VALUE): Result<Unit> {
        return try {
            val transformedValue = requireNotNull(transformer).invoke(key, value)
            requireNotNull(producer).sendMessage(key, transformedValue)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override val key: AttributeKey<DeadLetterQueueTransformerPlugin<SOURCE_VALUE, TARGET_VALUE>> =
        AttributeKey("dead-letter")

    override fun install(
        pipeline: Pipeline<*, ApplicationCall>,
        configure: DeadLetterTransformerConfig<SOURCE_VALUE, TARGET_VALUE>.() -> Unit
    ): DeadLetterQueueTransformerPlugin<SOURCE_VALUE, TARGET_VALUE> {
        val configuration = DeadLetterTransformerConfig<SOURCE_VALUE, TARGET_VALUE>()
        configuration.configure()

        val appEnv = requireNotNull(configuration.appEnv)

        transformer = configuration.transformer
        producer = KafkaSoknadsstatusProducer(appEnv, requireNotNull(configuration.targetSerde))

        DeadLetterQueueConsumerPlugin().install(pipeline) {
            deadLetterQueueConsumer =
                DeadLetterQueueConsumerImpl<SOURCE_VALUE>(
                    topic = requireNotNull(appEnv.deadLetterQueueTopic),
                    kafkaConsumer = KafkaUtils.createConsumer(
                        appEnv,
                        requireNotNull(configuration.sourceSerde)
                    ),
                    block = block,
                    pollDurationMs = appEnv.deadLetterQueueConsumerPollIntervalMs,
                    deadLetterMessageSkipService = DeadLetterMessageSkipServiceImpl(
                        DeadLetterMessageRepository(
                            requireNotNull(appEnv.deadLetterQueueSkipTableName),
                            requireNotNull(configuration.skipTableDataSource)
                        )
                    )
                )
        }

        return this
    }
}
