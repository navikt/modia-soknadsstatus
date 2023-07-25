package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import io.ktor.server.cio.*
import no.nav.modia.soknadsstatus.behandling.Behandling
import no.nav.modia.soknadsstatus.kafka.AppEnv
import no.nav.modia.soknadsstatus.kafka.DeadLetterQueueMetricsGaugeImpl
import no.nav.modia.soknadsstatus.kafka.DeadLetterQueueProducer
import no.nav.modia.soknadsstatus.kafka.SendToDeadLetterQueueExceptionHandler
import no.nav.personoversikt.common.ktor.utils.KtorServer

fun main() {
    runApp()
}

fun runApp(port: Int = 8080) {
    val config = AppEnv()
    val dlqMetricsGauge = DeadLetterQueueMetricsGaugeImpl(requireNotNull(config.deadLetterQueueMetricsGaugeName))
    val deadLetterProducer = DeadLetterQueueProducer(config, dlqMetricsGauge)
    val datasourceConfiguration = DatasourceConfiguration(DatasourceEnv((config.appName)))
    datasourceConfiguration.runFlyway()

    KtorServer.create(
        factory = CIO,
        port = port,
        application = {
            install(BaseNaisApp)
            install(KafkaStreamTransformPlugin<Behandling, SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering>()) {
                appEnv = config
                deadLetterQueueProducer = deadLetterProducer
                deserializationExceptionHandler = SendToDeadLetterQueueExceptionHandler()
                domainSerde = BehandlingXmlSerdes.XMLSerde()
                targetSerde = SoknadsstatusDomain.SoknadsstatusInkommendeOppdateringSerde()
                configure { stream ->
                    stream
                        .filter(::filter)
                        .mapValues(::transform)
                }
            }
            install(DeadLetterQueueTransformerPlugin<Behandling, SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering>()) {
                appEnv = config
                domainSerde = BehandlingXmlSerdes.XMLSerde()
                targetSerde = SoknadsstatusDomain.SoknadsstatusInkommendeOppdateringSerde()
                transformer = ::transform
                filter = ::filter
                skipTableDataSource = datasourceConfiguration.datasource
                deadLetterQueueMetricsGauge = dlqMetricsGauge
            }
        }
    ).start(wait = true)
}

fun filter(key: String?, value: Behandling): Boolean {
    Transformer.behandlingsStatus(value) ?: return false

    return Filter.filtrerBehandling(value)
}

fun transform(key: String?, behandling: Behandling) = Transformer.transform(behandling)
