package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import no.nav.modia.soknadsstatus.kafka.KafkaUtils
import no.nav.personoversikt.common.utils.SelftestGenerator
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration.Companion.seconds

class KafkaStreamConfig {
    var appname: String? = null
    var brokerUrl: String? = null
    var topology: (StreamsBuilder.() -> Unit)? = null

    fun topology(fn: StreamsBuilder.() -> Unit) {
        this.topology = fn
    }
}

val KafkaStreamPlugin = createApplicationPlugin("kafka-stream", ::KafkaStreamConfig) {
    val stream = KafkaUtils.createStream(
        applicationId = requireNotNull(pluginConfig.appname),
        brokerUrl = requireNotNull(pluginConfig.brokerUrl),
        configure = requireNotNull(pluginConfig.topology),
    )
    stream.start()
    registerShutdownhook { stream.close() }

    val kafkaReporter = SelftestGenerator.Reporter("kafka-stream", true)
    fixedRateTimer("kafka-stream", daemon = true, initialDelay = 0L, period = 10.seconds.inWholeMilliseconds) {
        when (stream.state()) {
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
