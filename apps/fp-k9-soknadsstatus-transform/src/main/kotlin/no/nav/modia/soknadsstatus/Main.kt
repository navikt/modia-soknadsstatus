package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import io.ktor.server.cio.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json
import no.nav.modia.soknadsstatus.behandling.Behandling
import no.nav.modia.soknadsstatus.behandling.BehandlingAvsluttet
import no.nav.modia.soknadsstatus.behandling.BehandlingOpprettet
import no.nav.personoversikt.common.ktor.utils.KtorServer
import no.nav.personoversikt.common.logging.Logging.secureLog
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
                        .mapValues(::decodeDtoContract)
                        .filter(::filter)
                        .mapValues(::transform)
                }
            }
        }
    ).start(wait = true)
}

fun filter(key: String?, behandling: Behandling?): Boolean {
    if (behandling == null) return false
    behandlingsStatus(behandling) ?: return false

    return Filter.filtrerBehandling(behandling)
}

fun decodeDtoContract(key: String?, value: String): Behandling? {
    return try {
        Json.decodeFromString(BehandlingSerializer, value)
    } catch (e: Exception) {
        secureLog.error("Failed to parse to external domain", e)
        null
    }
}

fun transform(key: String?, value: Behandling?): soknadsstatusDomain.soknadsstatusInnkommendeOppdatering {
    val behandling = value!!

    return soknadsstatusDomain.soknadsstatusInnkommendeOppdatering(
        aktorIder = behandling.aktoerREF.map { it.aktoerId },
        tema = behandling.sakstema.value,
        behandlingsRef = behandling.primaerBehandlingREF!!.behandlingsREF,
        systemRef = behandling.applikasjonSakREF,
        status = behandlingsStatus(behandling)!!,
        tidspunkt = behandling.hendelsesTidspunkt.toInstant(TimeZone.currentSystemDefault())
    )
}

private fun behandlingsStatus(behandling: Behandling): soknadsstatusDomain.Status? {
    if (behandling is BehandlingOpprettet) {
        return soknadsstatusDomain.Status.UNDER_BEHANDLING
    } else if (behandling is BehandlingAvsluttet) {
        return when (behandling.avslutningsstatus.value) {
            "avsluttet" -> soknadsstatusDomain.Status.FERDIG_BEHANDLET
            "avbrutt" -> soknadsstatusDomain.Status.AVBRUTT
            else -> {
                secureLog.error("Ukjent behandlingsstatus mottatt: ${behandling.avslutningsstatus.value}")
                null
            }
        }
    }
    return null
}
