package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import io.ktor.server.cio.*
import kotlinx.datetime.Clock
import no.nav.personoversikt.common.ktor.utils.KtorServer
import no.nav.personoversikt.common.utils.EnvUtils

fun main() {
    runApp()
}

data class Configuration(
    val appname: String = EnvUtils.getRequiredConfig("APP_NAME"),
    val appversion: String = EnvUtils.getRequiredConfig("APP_VERSION"),
    val brokerUrl: String = EnvUtils.getRequiredConfig("KAFKA_BROKER_URL"),
    val sourceTopic: String = EnvUtils.getRequiredConfig("KAFKA_SOURCE_TOPIC"),
    val targetTopic: String = EnvUtils.getRequiredConfig("KAFKA_TARGET_TOPIC"),
)

fun runApp(port: Int = 8080) {
    val config = Configuration()
    KtorServer.create(
        factory = CIO,
        port = port,
        application = {
            install(BaseNaisApp)
            install(KafkaStreamTransformPlugin) {
                appname = config.appname
                brokerUrl = config.brokerUrl
                sourceTopic = config.sourceTopic
                targetTopic = config.targetTopic
                configure { stream ->
                    stream
                        .mapValues(::deserialize)
                        .filter(::filter)
                        .mapValues(::transform)
                }
            }
        }
    ).start(wait = true)
}

class IntermediateFormat
fun deserialize(key: String?, value: String): IntermediateFormat {
    return IntermediateFormat()
}
fun filter(key: String?, value: IntermediateFormat): Boolean {
    return true
}

fun transform(key: String?, value: IntermediateFormat): SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering {
    // TODO fix mapping
    return SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering(
        aktorIder = listOf("123"),
        tema = "DAG",
        status = SoknadsstatusDomain.Status.UNDER_BEHANDLING,
        behandlingsId = "",
        systemRef = "arena",
        tidspunkt = Clock.System.now()
    )
}
