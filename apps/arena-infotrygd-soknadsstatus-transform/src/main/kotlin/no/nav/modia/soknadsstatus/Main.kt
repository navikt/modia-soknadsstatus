package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import io.ktor.server.cio.*
import kotlinx.serialization.json.Json
import no.nav.modia.soknadsstatus.behandling.Hendelse
import no.nav.modia.soknadsstatus.kafka.*
import no.nav.personoversikt.common.ktor.utils.KtorServer
import no.nav.personoversikt.common.logging.TjenestekallLogg

fun main() {
    runApp()
}

fun runApp(port: Int = 8080) {
    var runConsumer = true
    val config = AppEnv()
    val deadLetterProducer = DeadLetterQueueProducerImpl(config)
    val datasourceConfiguration = DatasourceConfiguration(DatasourceEnv((config.appName)))
    val slackClient = config.slackWebHookUrl?.let { SlackClient(it) }
    datasourceConfiguration.runFlyway()

    KtorServer
        .create(
            factory = CIO,
            port = port,
            application = {
                install(BaseNaisApp)
                if (runConsumer) {
                    install(KafkaStreamTransformPlugin<Hendelse, InnkommendeHendelse>()) {
                        appEnv = config
                        deserializationExceptionHandler =
                            SendToDeadLetterQueueExceptionHandler(
                                dlqProducer = deadLetterProducer,
                                topic = requireNotNull(config.deadLetterQueueTopic),
                                slackClient = slackClient,
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
                                .filter(::checkIfLegalInfotrygdHendelse)
                                .mapValues(::transform)
                        }
                    }
                    install(DeadLetterQueueTransformerPlugin<Hendelse, InnkommendeHendelse>()) {
                        appEnv = config
                        transformer = ::transform
                        filter = ::checkIfLegalInfotrygdHendelse
                        skipTableDataSource = datasourceConfiguration.datasource
                        deserializer = ::deserialize
                        serializer = ::serialize
                    }
                }
            },
        ).start(wait = true)
}

fun serialize(
    key: String?,
    value: InnkommendeHendelse,
) = Json.encodeToString(
    InnkommendeHendelse.serializer(),
    value,
)

fun deserialize(
    key: String?,
    value: String,
): Hendelse =
    try {
        BehandlingDeserializer.deserialize(value)
    } catch (e: Exception) {
        TjenestekallLogg.error(
            "Klarte ikke å håndtere DL",
            fields = mapOf("key" to key, "value" to value),
            throwable = e,
        )
        throw e
    }

fun transform(
    key: String?,
    hendelse: Hendelse,
) = Transformer.transform(
    hendelse = hendelse,
    identer = hendelse.aktoerREF.map { it.aktoerId },
    statusMapper = ArenaInfotrygdAvslutningsstatusMapper,
    hendelseType = SoknadsstatusDomain.HendelseType.convertFromString(hendelse.hendelseType),
)

// Infotrygd sender hendelser av typen BehandlingOpprettetOgAvsluttet med behandlingstype = "" og avslutningstatus = "blank" om det er en feilregistrering
fun checkIfLegalInfotrygdHendelse(
    key: String?,
    hendelse: Hendelse,
) = !(
    hendelse.hendelseType == "BEHANDLING_OPPRETTET_OG_AVSLUTTET" && hendelse.hendelsesprodusentREF.value == INFOTRYGD &&
        hendelse.behandlingstype?.value == "" && hendelse.avslutningsstatus?.value == "blank"
)
