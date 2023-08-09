package no.nav.modia.soknadsstatus

import Filter
import io.ktor.server.application.*
import io.ktor.server.cio.*
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.json.Json
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.*
import no.nav.modia.soknadsstatus.kafka.*
import no.nav.personoversikt.common.ktor.utils.KtorServer
import no.nav.personoversikt.common.logging.Logging.secureLog
import no.nav.personoversikt.common.logging.TjenestekallLogg

fun main() {
    runApp()
}

fun runApp(port: Int = 8080) {
    val config = AppEnv()
    val dlqMetricsGauge = DeadLetterQueueMetricsGaugeImpl(requireNotNull(config.deadLetterQueueMetricsGaugeName))
    val deadLetterProducer = DeadLetterQueueProducerImpl(config, dlqMetricsGauge)
    val datasourceConfiguration = DatasourceConfiguration(DatasourceEnv((config.appName)))
    datasourceConfiguration.runFlyway()

    KtorServer.create(
        factory = CIO,
        port = port,
        application = {
            install(BaseNaisApp)
            install(KafkaStreamTransformPlugin<Hendelse, SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering>()) {
                appEnv = config
                deserializationExceptionHandler = SendToDeadLetterQueueExceptionHandler(
                    dlqProducer = deadLetterProducer,
                    topic = requireNotNull(config.deadLetterQueueTopic)
                )
                sourceTopic = requireNotNull(config.sourceTopic)
                targetTopic = requireNotNull(config.targetTopic)
                deserializer = ::deserialize
                serializer = ::serialize
                onSerializationException = { record, exception ->
                    TjenestekallLogg.error(
                        "Klarte ikke å serialisere melding",
                        fields = mapOf("key" to record.key(), "behandlingsId" to record.value()?.behandlingsId),
                        throwable = exception
                    )
                }
                configure { stream ->
                    stream
                        .filter(::filter)
                        .mapValues(::transform)
                }
            }
            install(DeadLetterQueueTransformerPlugin<Hendelse, SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering>()) {
                appEnv = config
                transformer = ::transform
                filter = ::filter
                skipTableDataSource = datasourceConfiguration.datasource
                deadLetterQueueMetricsGauge = dlqMetricsGauge
                deserializer = ::deserialize
                serializer = ::serialize
            }
        }
    ).start(wait = true)
}

fun serialize(key: String?, value: SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering) = Json.encodeToString(
    SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering.serializer(),
    value
)

fun deserialize(key: String?, value: String) = BehandlingDeserializer.deserialize(value)

fun filter(key: String?, value: Hendelse): Boolean {
    behandlingsStatus(value) ?: return false

    return Filter.filtrerBehandling(value as BehandlingStatus)
}

fun transform(key: String?, hendelse: Hendelse?): SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering {
    val behandlingStatus = hendelse as BehandlingStatus
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
        "avsluttet", "ok", "ja" -> SoknadsstatusDomain.Status.FERDIG_BEHANDLET
        "avbrutt", "nei", "no" -> SoknadsstatusDomain.Status.AVBRUTT
        else -> {
            secureLog.error("Ukjent behandlingsstatus mottatt: ${avslutningsstatus.value}")
            null
        }
    }
}
