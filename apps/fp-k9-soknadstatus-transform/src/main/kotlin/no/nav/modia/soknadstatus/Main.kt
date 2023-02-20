package no.nav.modia.soknadstatus

import io.ktor.server.application.*
import io.ktor.server.cio.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json
import no.nav.modia.soknadstatus.kafka.BehandlingAvsluttet
import no.nav.modia.soknadstatus.kafka.BehandlingOpprettet
import no.nav.personoversikt.common.ktor.utils.KtorServer
import no.nav.personoversikt.common.logging.Logging.secureLog
import no.nav.personoversikt.common.utils.EnvUtils
import java.lang.IllegalArgumentException

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
                        .mapValues(::transform)
                        .filter(::filter)
                }
            }
        }
    ).start(wait = true)
}

fun filter(key: String?, value: SoknadstatusDomain.SoknadstatusOppdatering?): Boolean {
    if (value == null) return false
    return true
}

fun transform(key: String?, value: String): SoknadstatusDomain.SoknadstatusOppdatering? {
    return try {
        val decodedMessage = Json.decodeFromString(BehandlingSerializer, value)

        var status: SoknadstatusDomain.Status? = null

        if (decodedMessage is BehandlingOpprettet) {
            status = SoknadstatusDomain.Status.UNDER_BEHANDLING
        } else if (decodedMessage is BehandlingAvsluttet) {
            status = behandlingsStatus(decodedMessage.avslutningsstatus.value)
        }

        SoknadstatusDomain.SoknadstatusOppdatering(
            ident = decodedMessage.aktoerREF.first().aktoerId,
            tema = decodedMessage.sakstema.value,
            behandlingsRef = decodedMessage.primaerBehandlingREF.behandlingsREF,
            systemRef = decodedMessage.applikasjonSakREF,
            status = status,
            tidspunkt = decodedMessage.hendelsesTidspunkt.toInstant(TimeZone.currentSystemDefault())
        )
    } catch (e: Exception) {
        secureLog.error("Failed to parse message", e)
        null
    }
}

private fun behandlingsStatus(status: String): SoknadstatusDomain.Status? {
    return when (status) {
        "avsluttet" -> SoknadstatusDomain.Status.FERDIG_BEHANDLET
        "avbrutt" -> SoknadstatusDomain.Status.AVBRUTT
        else -> {
            throw IllegalArgumentException("Ukjent behandlingsstatus mottatt: $status")
        }
    }
}
