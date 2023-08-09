package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import io.ktor.server.cio.*
import no.nav.modia.soknadsstatus.behandling.Behandling
import no.nav.modia.soknadsstatus.kafka.AppEnv
import no.nav.modia.soknadsstatus.kafka.DeadLetterQueueMetricsGaugeImpl
import no.nav.modia.soknadsstatus.kafka.SendToDeadLetterQueueExceptionHandler
import kotlinx.serialization.json.Json
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
            install(DeadLetterQueueTransformerPlugin<Behandling, SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering>()) {
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

fun filter(key: String?, value: Behandling): Boolean {
    Transformer.behandlingsStatus(value) ?: return false

    return Filter.filtrerBehandling(value)
}

fun transform(key: String?, behandling: Behandling) = Transformer.transform(behandling)
