package no.nav.modia.soknadstatus

import no.nav.modia.soknadstatus.jms.Jms
import no.nav.personoversikt.common.utils.EnvUtils

private const val examples = "apps/data-generator-app/src/main/resources/examples"
class Configuration(
    val brokerUrl: String = "localhost:9092",
    val soknadstatusTopic: String = "modia-soknadstatus",
    val sources: List<Source> = listOf(
        Source(name = "Infotrygd/Arena", type = Source.Type.JMS, resourceId = "infotrygd-soknadstatus", exampleFile = "$examples/infotrygd.xml"),
        Source(name = "Foreldrepenger/Pleiepenger", type = Source.Type.KAFKA, resourceId = "aapen-sob-oppgaveHendelse-v1", exampleFile = "$examples/pf-k9.json"),
    ),
    val handlers: Map<Source.Type, PostHandler> = mapOf(
        Source.Type.JMS to JmsHandler(
            Jms.Config(
                host = EnvUtils.getRequiredConfig("JMS_HOST"),
                port = EnvUtils.getRequiredConfig("JMS_PORT").toInt(),
                queueManager = EnvUtils.getRequiredConfig("JMS_QUEUEMANAGER"),
                username = EnvUtils.getRequiredConfig("JMS_USERNAME"),
                password = EnvUtils.getRequiredConfig("JMS_PASSWORD"),
            )
        ),
        Source.Type.KAFKA to KafkaHandler(
            EnvUtils.getRequiredConfig("KAFKA_BROKER_URL")
        )
    )
)
