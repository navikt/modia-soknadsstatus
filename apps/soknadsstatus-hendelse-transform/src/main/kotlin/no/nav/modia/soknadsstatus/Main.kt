package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import io.ktor.server.cio.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json
import no.nav.modia.soknadsstatus.behandling.Behandling
import no.nav.modia.soknadsstatus.behandling.BehandlingAvsluttet
import no.nav.modia.soknadsstatus.behandling.BehandlingOpprettet
import no.nav.modia.soknadsstatus.kafka.*
import no.nav.personoversikt.common.ktor.utils.KtorServer
import no.nav.personoversikt.common.logging.Logging.secureLog
import no.nav.personoversikt.common.logging.TjenestekallLogg

fun main() {
    runApp()
}

fun runApp(port: Int = 8080) {
    val config = AppEnv()
    val dlqMetricsGauge =
        DeadLetterQueueMetricsGaugeImpl(requireNotNull(config.deadLetterQueueMetricsGaugeName))
    val deadLetterProducer = DeadLetterQueueProducerImpl(config, dlqMetricsGauge)
    val datasourceConfiguration = DatasourceConfiguration(DatasourceEnv(appName = config.appName))
    datasourceConfiguration.runFlyway()

    KtorServer.create(
        factory = CIO,
        port = port,
        application = {
            install(BaseNaisApp)
            install(KafkaStreamTransformPlugin<Behandling, SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering>()) {
                deserializationExceptionHandler = SendToDeadLetterQueueExceptionHandler(
                    topic = requireNotNull(config.deadLetterQueueTopic),
                    dlqProducer = deadLetterProducer
                )
                sourceTopic = requireNotNull(config.sourceTopic)
                targetTopic = requireNotNull(config.targetTopic)
                deserializer = { _, value -> Json.decodeFromString(Behandling.serializer(), value) }
                serializer = { _, value ->
                    Json.encodeToString(
                        SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering.serializer(),
                        value
                    )
                }
                onSerializationException = { record, exception ->
                    TjenestekallLogg.error(
                        "Klarte ikke å serialisere melding",
                        fields = mapOf("key" to record.key(), "behandlingsId" to record.value()?.behandlingsId),
                        throwable = exception
                    )
                }
                configure { stream ->
                    stream.filter(::filter).mapValues(::transform)
                }
            }
            install(DeadLetterQueueTransformerPlugin<Behandling, SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering>()) {
                appEnv = config
                transformer = ::transform
                filter = ::filter
                skipTableDataSource = datasourceConfiguration.datasource
                deadLetterQueueMetricsGauge = dlqMetricsGauge
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
