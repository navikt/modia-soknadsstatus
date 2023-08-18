package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import io.ktor.server.cio.*
import kotlinx.serialization.json.Json
import no.nav.api.generated.pdl.enums.IdentGruppe
import no.nav.modia.soknadsstatus.behandling.Behandling
import no.nav.modia.soknadsstatus.kafka.*
import no.nav.personoversikt.common.ktor.utils.KtorServer
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
            install(KafkaStreamTransformPlugin<Behandling, SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering>()) {
                appEnv = config
                deserializationExceptionHandler = SendToDeadLetterQueueExceptionHandler(
                    dlqProducer = deadLetterProducer,
                    topic = requireNotNull(config.deadLetterQueueTopic),
                )
                sourceTopic = requireNotNull(config.sourceTopic)
                targetTopic = requireNotNull(config.targetTopic)
                deserializer = ::deserialize
                serializer = ::serialize
                onSerializationException = { record, exception ->
                    TjenestekallLogg.error(
                        "Klarte ikke å serialisere melding",
                        fields = mapOf("key" to record.key(), "behandlingsId" to record.value()?.behandlingsId),
                        throwable = exception,
                    )
                }
                configure { stream ->
                    stream
                        .mapValues(::transform)
                }
            }
            install(DeadLetterQueueTransformerPlugin<Behandling, SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering>()) {
                appEnv = config
                transformer = ::transform
                skipTableDataSource = datasourceConfiguration.datasource
                deadLetterQueueMetricsGauge = dlqMetricsGauge
                deserializer = ::deserialize
                serializer = ::serialize
            }
        },
    ).start(wait = true)
}

fun serialize(key: String?, value: SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering) = Json.encodeToString(
    SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering.serializer(),
    value,
)

fun deserialize(key: String?, value: String): Behandling {
    return try {
        BehandlingDeserializer.deserialize(value)
    } catch (e: Exception) {
        TjenestekallLogg.error(
            "Klarte ikke å håndtere DL",
            fields = mapOf("key" to key, "value" to value),
            throwable = e
        )
        throw e
    }
}

fun transform(key: String?, behandling: Behandling) = Transformer.transform(
    behandling = behandling,
    statusMapper = InfotrygdAvslutningsstatusMapper,
    identer = behandling.aktoerREF.map {
        SoknadsstatusDomain.IdentType(
            ident = it.aktoerId,
            type = IdentGruppe.FOLKEREGISTERIDENT
        )
    }
)
