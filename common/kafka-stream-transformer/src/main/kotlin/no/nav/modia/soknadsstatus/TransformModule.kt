package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.kafka.streams.kstream.KStream

class KafkaStreamTransformConfig {
    var appname: String? = null
    var brokerUrl: String? = null
    var configure: ((KStream<String, String>) -> KStream<String, SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering?>)? = null

    fun configure(fn: (KStream<String, String>) -> KStream<String, SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering?>) {
        this.configure = fn
    }

    var sourceTopic: String? = null
    var targetTopic: String? = null
}

val KafkaStreamTransformPlugin = createApplicationPlugin("kafka-stream-transform", ::KafkaStreamTransformConfig) {
    val applicationName = requireNotNull(pluginConfig.appname)
    val kafkaBrokerUrl = requireNotNull(pluginConfig.brokerUrl)
    val sourceTopic = requireNotNull(pluginConfig.sourceTopic)
    val configure = requireNotNull(pluginConfig.configure)
    val targetTopic = pluginConfig.targetTopic

    with(application) {
        install(KafkaStreamPlugin) {
            appname = applicationName
            brokerUrl = kafkaBrokerUrl
            topology {
                val stream = stream<String, String>(sourceTopic)
                    .let(configure)
                    .mapValues(::serialize)
                if (targetTopic != null) {
                    stream?.to(targetTopic)
                }
            }
        }
    }
}

private fun serialize(key: String?, value: SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering?): String {
    return Json.encodeToString(value)
}
