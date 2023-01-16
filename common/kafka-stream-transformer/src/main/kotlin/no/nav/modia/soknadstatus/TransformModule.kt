package no.nav.modia.soknadstatus

import io.ktor.server.application.*
import io.ktor.server.engine.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.personoversikt.common.ktor.utils.Metrics
import no.nav.personoversikt.common.ktor.utils.Selftest
import no.nav.personoversikt.common.utils.EnvUtils
import no.nav.personoversikt.common.utils.SelftestGenerator
import org.apache.kafka.common.serialization.Serdes.StringSerde
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.errors.DefaultProductionExceptionHandler
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler
import org.apache.kafka.streams.kstream.KStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration.Companion.seconds

val log: Logger = LoggerFactory.getLogger("kafka-stream-transformer")

data class Configuration(
    val appname: String = EnvUtils.getRequiredConfig("APP_NAME"),
    val appversion: String = EnvUtils.getRequiredConfig("APP_VERSION"),
    val kafkaConfiguration: KafkaConfiguration = KafkaConfiguration(),
    val sourceTopic: String = EnvUtils.getRequiredConfig("KAFKA_SOURCE_TOPIC"),
    val targetTopic: String = EnvUtils.getRequiredConfig("KAFKA_TARGET_TOPIC"),
) {

    data class KafkaConfiguration(
        val brokerUrl: String = EnvUtils.getRequiredConfig("KAFKA_BROKER_URL"),
    )
}

fun Application.kafkaStreamModule(
    config: Configuration,
    configure: StreamsBuilder.() -> Unit
) {
    val kafkaStream = createStream(
        config = config,
        configure = configure,
    )
    kafkaStream.start()

    val kafkaReporter = SelftestGenerator.Reporter("kafka-stream", true)
    fixedRateTimer("kafka-stream", daemon = true, initialDelay = 0L, period = 10.seconds.inWholeMilliseconds) {
        when (kafkaStream.state()) {
            KafkaStreams.State.CREATED -> kafkaReporter.reportOk()
            KafkaStreams.State.REBALANCING -> kafkaReporter.reportOk()
            KafkaStreams.State.RUNNING -> kafkaReporter.reportOk()
            KafkaStreams.State.NOT_RUNNING -> kafkaReporter.reportError(Exception("KStream is not running"))
            KafkaStreams.State.PENDING_ERROR -> kafkaReporter.reportError(Exception("KStream is pending error"))
            KafkaStreams.State.ERROR -> kafkaReporter.reportError(Exception("KStream has error"))
            KafkaStreams.State.PENDING_SHUTDOWN, null -> {}
        }
    }

    install(Metrics.Plugin)
    install(Selftest.Plugin) {
        appname = config.appname
        version = config.appversion
    }

    install(ShutDownUrl.ApplicationCallPlugin) {
        shutDownUrl = "/shutdown"
        exitCodeSupplier = {
            kafkaStream.close()
            0
        }
    }
}

fun Application.kafkaStreamTransformModule(
    config: Configuration,
    configure: (KStream<String, String>) -> KStream<String, SoknadstatusDomain.Soknadstatus>
) {
    kafkaStreamModule(config) {
        stream<String, String>(config.sourceTopic)
            .let(configure)
            .mapValues(::serialize)
    }
}

private fun serialize(key: String?, value: SoknadstatusDomain.Soknadstatus): String {
    return Json.encodeToString(value)
}

private fun createStream(
    config: Configuration,
    configure: StreamsBuilder.() -> Unit
): KafkaStreams {
    val props = Properties()
    props[StreamsConfig.APPLICATION_ID_CONFIG] = config.appname
    props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = config.kafkaConfiguration.brokerUrl
    props[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = StringSerde().javaClass
    props[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = StringSerde().javaClass
    props[StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG] = LogAndFailExceptionHandler::class.java
    props[StreamsConfig.DEFAULT_PRODUCTION_EXCEPTION_HANDLER_CLASS_CONFIG] = DefaultProductionExceptionHandler::class.java

    val builder = StreamsBuilder()
    builder.apply(configure)

    val topology = builder.build()
    log.info(
        """
            Created KStream: 
            ${topology.describe()}
        """.trimIndent()
    )

    return KafkaStreams(topology, props)
}
