package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import io.ktor.server.cio.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import no.nav.modia.soknadsstatus.behandling.Behandling
import no.nav.modia.soknadsstatus.behandling.BehandlingAvsluttet
import no.nav.modia.soknadsstatus.behandling.BehandlingJsonSerdes
import no.nav.modia.soknadsstatus.behandling.BehandlingOpprettet
import no.nav.modia.soknadsstatus.kafka.*
import no.nav.personoversikt.common.ktor.utils.KtorServer
import no.nav.personoversikt.common.logging.Logging.secureLog

fun main() {
    runApp()
}

fun runApp(port: Int = 8080) {
    val config = AppEnv()
    val deadLetterProducer = DeadLetterQueueProducer(config)
    KtorServer.create(
        factory = CIO,
        port = port,
        application = {
            install(BaseNaisApp)
            install(KafkaStreamTransformPlugin<Behandling, SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering>()) {
                appEnv = config
                domainSerde = BehandlingJsonSerdes.JsonSerde()
                targetSerde = SoknadsstatusDomain.SoknadsstatusInkommendeOppdateringSerde()
                deserializationExceptionHandler = SendToDeadLetterQueueExceptionHandler()
                deadLetterQueueProducer = deadLetterProducer
                configure { stream ->
                    stream.filter(::filter).mapValues(::transform)
                }
            }
            install(DeadLetterQueueTransformerPlugin<Behandling, SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering>()) {
                appEnv = config
                domainSerde = BehandlingJsonSerdes.JsonSerde()
                targetSerde = SoknadsstatusDomain.SoknadsstatusInkommendeOppdateringSerde()
                transformer = ::transform
                filter = ::filter
                skipTableDataSource = DatasourceConfiguration(appMode = config.appMode, appName = config.appName, datasourceEnv = DatasourceEnv()).datasource
            }
        }
    ).start(wait = true)
}

fun filter(key: String?, behandling: Behandling?): Boolean {
    if (behandling == null) return false
    behandlingsStatus(behandling) ?: return false

    return Filter.filtrerBehandling(behandling)
}

fun transform(key: String?, value: Behandling?): SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering {
    val behandling = value!!

    return SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering(
        aktorIder = behandling.aktoerREF.map { it.aktoerId },
        tema = behandling.sakstema.value,
        behandlingsId = behandling.behandlingsID,
        systemRef = behandling.applikasjonSakREF,
        status = behandlingsStatus(behandling)!!,
        tidspunkt = behandling.hendelsesTidspunkt.toInstant(TimeZone.currentSystemDefault())
    )
}

private fun behandlingsStatus(behandling: Behandling): SoknadsstatusDomain.Status? {
    if (behandling is BehandlingOpprettet) {
        return SoknadsstatusDomain.Status.UNDER_BEHANDLING
    } else if (behandling is BehandlingAvsluttet) {
        return when (behandling.avslutningsstatus.value.lowercase()) {
            "avsluttet", "ok" -> SoknadsstatusDomain.Status.FERDIG_BEHANDLET
            "avbrutt" -> SoknadsstatusDomain.Status.AVBRUTT
            else -> {
                secureLog.error("Ukjent behandlingsstatus mottatt: ${behandling.avslutningsstatus.value}")
                null
            }
        }
    }
    return null
}
