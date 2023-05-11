package no.nav.modia.soknadsstatus

import Filter
import io.ktor.server.application.*
import io.ktor.server.cio.*
import kotlinx.datetime.toKotlinInstant
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.*
import no.nav.modia.soknadsstatus.behandling.Behandling
import no.nav.modia.soknadsstatus.kafka.AppEnv
import no.nav.modia.soknadsstatus.kafka.DeadLetterQueueProducer
import no.nav.modia.soknadsstatus.kafka.SendToDeadLetterQueueExceptionHandler
import no.nav.personoversikt.common.ktor.utils.KtorServer
import no.nav.personoversikt.common.logging.Logging
import org.apache.kafka.common.serialization.Serdes.StringSerde

fun main() {
    runApp()
}

fun runApp(port: Int = 8080) {
    val config = AppEnv()
    val deadLetterProducer = DeadLetterQueueProducer(config, StringSerde())

    KtorServer.create(
        factory = CIO,
        port = port,
        application = {
            install(BaseNaisApp)
            install(KafkaStreamTransformPlugin<String, Hendelse, SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering>()) {
                appEnv = config
                deadLetterQueueProducer = deadLetterProducer
                deserializationExceptionHandler = SendToDeadLetterQueueExceptionHandler()
                dlqSerde = StringSerde()
                domainTypeserde = BehandlingXmlSerdes.XMLSerde()
                targetTypeSerde = SoknadsstatusDomain.SoknadsstatusInkommendeOppdateringSerde()
                configure { stream ->
                    stream
                        .filter(::filter)
                        .mapValues(::transform)
                }
            }
            install(DeadLetterQueueTransformerPlugin<Hendelse, SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering>()) {
                appEnv = config
                sourceSerde = BehandlingXmlSerdes.XMLSerde()
                targetSerde = SoknadsstatusDomain.SoknadsstatusInkommendeOppdateringSerde()
                transformer = ::transform
                filter = ::filter
                skipTableDataSource = DatasourceConfiguration().datasource
            }
        }
    ).start(wait = true)
}

fun filter(key: String?, value: Hendelse): Boolean {
    getBehandlingsStatus(value) ?: return false

    return Filter.filtrerBehandling(value as Behandling)
}

fun transform(key: String?, hendelse: Hendelse?): SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering {
    val behandlingStatus = hendelse as BehandlingStatus
    return SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering(
        aktorIder = behandlingStatus.aktoerREF.map { it.aktoerId },
        tema = behandlingStatus.sakstema.value,
        behandlingsId = behandlingStatus.behandlingsID,
        systemRef = behandlingStatus.hendelsesprodusentREF.value,
        status = getBehandlingsStatus(behandlingStatus)!!,
        tidspunkt = behandlingStatus.hendelsesTidspunkt.toGregorianCalendar().toInstant().toKotlinInstant()
    )
}

private fun getBehandlingsStatus(hendelse: Hendelse): SoknadsstatusDomain.Status? {
    return when (hendelse) {
        is BehandlingOpprettet -> SoknadsstatusDomain.Status.UNDER_BEHANDLING
        is BehandlingOpprettetOgAvsluttet -> behandlingAvsluttetStatus(hendelse.avslutningsstatus)
        is BehandlingAvsluttet -> behandlingAvsluttetStatus(hendelse.avslutningsstatus)
        else -> {
            Logging.secureLog.error("Ukjent Hendelse mottatt: $hendelse")
            null
        }
    }
}

private fun behandlingAvsluttetStatus(avslutningsstatus: Avslutningsstatuser): SoknadsstatusDomain.Status? {
    return when (avslutningsstatus.value.lowercase()) {
        "avsluttet", "ok" -> SoknadsstatusDomain.Status.FERDIG_BEHANDLET
        "avbrutt" -> SoknadsstatusDomain.Status.AVBRUTT
        else -> {
            Logging.secureLog.error("Ukjent behandlingsstatus mottatt: ${avslutningsstatus.value}")
            null
        }
    }
}
