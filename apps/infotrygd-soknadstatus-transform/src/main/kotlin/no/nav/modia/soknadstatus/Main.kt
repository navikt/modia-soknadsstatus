package no.nav.modia.soknadstatus

import io.ktor.server.application.*
import io.ktor.server.cio.*
import kotlinx.datetime.*
import no.nav.personoversikt.common.ktor.utils.KtorServer
import no.nav.personoversikt.common.utils.EnvUtils
import org.w3c.dom.Document
import javax.xml.parsers.DocumentBuilderFactory

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
                        .mapValues(::deserialize)
                        .filter(::filter)
                        .mapValues(::transform)
                }
            }
        }
    ).start(wait = true)
}

private val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
fun deserialize(key: String?, value: String): Document {
    return documentBuilder.parse(value.byteInputStream())
}

val godkjenteDokomentTyper = arrayOf("behandlingOpprettetOgAvsluttet")
fun filter(key: String?, value: Document): Boolean {
    return ETL.rootNode(value) in godkjenteDokomentTyper // TODO ?? && !ETL.behandlingsId(value).startsWith("17")
}

fun transform(key: String?, value: Document): SoknadstatusDomain.SoknadstatusOppdatering {
    // TODO fix mapping
    return SoknadstatusDomain.SoknadstatusOppdatering(
        ident = ETL.aktoerId(value),
        tema = ETL.sakstema(value),
        status = when (ETL.status(value)) {
            "innvilget" -> SoknadstatusDomain.Status.FERDIG_BEHANDLET
            "avbrutt" -> SoknadstatusDomain.Status.AVBRUTT
            else -> SoknadstatusDomain.Status.UNDER_BEHANDLING
        },
        behandlingsRef = ETL.behandlingsId(value),
        systemRef = "infotrygd",
        tidspunkt = LocalDateTime.parse(ETL.tidspunkt(value)).toInstant(TimeZone.currentSystemDefault())
    )
}
