package no.nav.modia.soknadsstatus

import no.nav.modia.soknadsstatus.jms.Jms
import no.nav.modia.soknadsstatus.kafka.AppEnv
import no.nav.personoversikt.common.utils.EnvUtils

private const val examples = "apps/data-generator-app/src/main/resources/examples"

class Configuration(
    val brokerUrl: String = "localhost:9092",
    val soknadsstatusTopic: String = "modia-soknadsstatus",
    val sources: List<Source> = listOf(
        Source(
            name = "Infotrygd/Arena",
            type = Source.Type.JMS,
            resourceId = "arena-infotrygd-soknadsstatus",
            exampleFile = "$examples/infotrygd.xml"
        ),
        Source(
            name = "Foreldrepenger/Pleiepenger",
            type = Source.Type.KAFKA,
            resourceId = "aapen-sob-oppgaveHendelse-v1",
            exampleFile = "$examples/pf-k9.json"
        ),
    )
)

class Handlers(appEnv: AppEnv) {
    val handlers: Map<Source.Type, PostHandler> = mapOf(
        Source.Type.JMS to JmsHandler(
            Jms.Config(
                host = EnvUtils.getRequiredConfig("JMS_HOST"),
                port = EnvUtils.getRequiredConfig("JMS_PORT").toInt(),
                queueManager = EnvUtils.getRequiredConfig("JMS_QUEUEMANAGER"),
                channel = EnvUtils.getRequiredConfig("JMS_CHANNEL"),
                username = EnvUtils.getRequiredConfig("JMS_USERNAME"),
                password = EnvUtils.getRequiredConfig("JMS_PASSWORD"),
            ),
            appEnv.appMode
        ),
        Source.Type.KAFKA to KafkaHandler(
            appEnv
        )
    )
}
