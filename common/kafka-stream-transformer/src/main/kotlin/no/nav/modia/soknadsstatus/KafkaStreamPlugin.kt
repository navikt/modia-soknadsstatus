package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import no.nav.modia.soknadsstatus.kafka.AppEnv
import no.nav.modia.soknadsstatus.kafka.KafkaUtils
import no.nav.personoversikt.common.utils.SelftestGenerator
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration.Companion.seconds

class KafkaStreamConfig {
    var appEnv: AppEnv? = null
    var topology: (StreamsBuilder.() -> Unit)? = null

    fun topology(fn: StreamsBuilder.() -> Unit) {
        this.topology = fn
    }
}

class KafkaStreamPlugin : Plugin<Pipeline<*, PipelineCall>, KafkaStreamConfig, KafkaStreamPlugin> {
    private var stream: KafkaStreams? = null

    override val key: AttributeKey<KafkaStreamPlugin> = AttributeKey("kafka-stream")

    override fun install(
        pipeline: Pipeline<*, PipelineCall>,
        configure: KafkaStreamConfig.() -> Unit,
    ): KafkaStreamPlugin {
        val configuration = KafkaStreamConfig()
        configuration.configure()

        stream =
            KafkaUtils.createStream(
                appConfig = requireNotNull(configuration.appEnv),
                configure = requireNotNull(configuration.topology),
            )

        stream?.start()
        registerShutdownhook { stream?.close() }
        registerReporter("kafka-stream", stream)
        return this
    }

    private fun registerReporter(
        name: String,
        stream: KafkaStreams?,
    ) {
        val kafkaReporter = SelftestGenerator.Reporter(name, true)
        fixedRateTimer(name, daemon = true, initialDelay = 0L, period = 10.seconds.inWholeMilliseconds) {
            when (stream?.state()) {
                KafkaStreams.State.CREATED -> kafkaReporter.reportOk()
                KafkaStreams.State.REBALANCING -> kafkaReporter.reportOk()
                KafkaStreams.State.RUNNING -> kafkaReporter.reportOk()
                KafkaStreams.State.NOT_RUNNING -> kafkaReporter.reportError(Exception("KStream is not running"))
                KafkaStreams.State.PENDING_ERROR -> kafkaReporter.reportError(Exception("KStream is pending error"))
                KafkaStreams.State.ERROR -> kafkaReporter.reportError(Exception("KStream has error"))
                KafkaStreams.State.PENDING_SHUTDOWN, null -> {}
            }
        }
    }
}
