package no.nav.modia.soknadstatus

import io.ktor.server.application.*
import io.ktor.server.cio.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json
import no.nav.modia.soknadstatus.kafka.BehandlingAvsluttet
import no.nav.modia.soknadstatus.kafka.BehandlingOpprettet
import no.nav.personoversikt.common.ktor.utils.KtorServer
import no.nav.personoversikt.common.utils.EnvUtils
import java.time.LocalDateTime

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
                        .filter(::filter)
                        .mapValues(::transform)
                }
            }
        }
    ).start(wait = true)
}

fun filter(key: String?, value: String): Boolean {
    return true
}

fun transform(key: String?, value: String): SoknadstatusDomain.SoknadstatusOppdatering {
    val decodedMessage = Json.decodeFromString(BehandlingAvsluttet.serializer(), value)
    // TODO fix mapping
    return SoknadstatusDomain.SoknadstatusOppdatering(
        ident = decodedMessage.aktoerREF.first().aktoerId,
        tema = decodedMessage.behandlingstema.value,
        behandlingsRef = decodedMessage.primaerBehandlingREF?.behandlingsREF ?: "",
        systemRef = "foreldrepenger",
        status = enumValueOf<SoknadstatusDomain.Status>(decodedMessage.avslutningsstatus.value) ?: SoknadstatusDomain.Status.UNDER_BEHANDLING,
        tidspunkt = decodedMessage.hendelsesTidspunkt.toInstant(TimeZone.currentSystemDefault())
    )
}
