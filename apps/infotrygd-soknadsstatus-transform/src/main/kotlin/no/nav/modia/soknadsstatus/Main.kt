package no.nav.modia.soknadsstatus

import Filter
import io.ktor.server.application.*
import io.ktor.server.cio.*
import kotlinx.datetime.toKotlinInstant
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.*
import no.nav.modia.soknadsstatus.behandling.Behandling
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
                        .mapValues(::mapXmlMessageToHendelse)
                        .filter(::filter)
                        .mapValues(::transform)
                }
            }
        }
    ).start(wait = true)
}

fun mapXmlMessageToHendelse(key: String?, value: String): Hendelse {
    return XMLConverter.fromXml(value)
}

fun filter(key: String?, value: Hendelse): Boolean {
    behandlingsStatus(value) ?: return false

    return Filter.filtrerBehandling(value as Behandling)
}

fun transform(key: String?, value: Hendelse?): SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering {
    checkNotNull(value)
    val behandlingStatus = value as BehandlingStatus
    return SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering(
        aktorIder = behandlingStatus.aktoerREF.map { it.aktoerId },
        tema = behandlingStatus.sakstema.value,
        behandlingsId = behandlingStatus.behandlingsID,
        systemRef = behandlingStatus.hendelsesprodusentREF.value,
        status = behandlingsStatus(behandlingStatus)!!,
        tidspunkt = behandlingStatus.hendelsesTidspunkt.toGregorianCalendar().toInstant().toKotlinInstant()
    )
}

private fun behandlingsStatus(hendelse: Hendelse): SoknadsstatusDomain.Status? {
    return when (hendelse) {
        is BehandlingOpprettet -> SoknadsstatusDomain.Status.UNDER_BEHANDLING
        is BehandlingOpprettetOgAvsluttet -> behandlingAvsluttetStatus(hendelse.avslutningsstatus)
        is BehandlingAvsluttet -> behandlingAvsluttetStatus(hendelse.avslutningsstatus)
        else -> {
            secureLog.error("Ukjent Hendelse mottatt: $hendelse")
            null
        }
    }
}

private fun behandlingAvsluttetStatus(avslutningsstatus: Avslutningsstatuser): SoknadsstatusDomain.Status? {
    return when (avslutningsstatus.value.lowercase()) {
        "avsluttet", "ok" -> SoknadsstatusDomain.Status.FERDIG_BEHANDLET
        "avbrutt" -> SoknadsstatusDomain.Status.AVBRUTT
        else -> {
            secureLog.error("Ukjent behandlingsstatus mottatt: ${avslutningsstatus.value}")
            null
        }
    }
}
