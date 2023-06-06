package no.nav.modia.soknadsstatus

import no.nav.modia.soknadsstatus.jms.Jms
import no.nav.personoversikt.common.utils.EnvUtils.getRequiredConfig

data class MqConfig(
    val mqQueue: String = getRequiredConfig("JMS_CHANNEL"),
    val config: Jms.Config = createMqConfig()
)

private fun createMqConfig() = Jms.Config(
    host = getRequiredConfig("JMS_HOST"),
    port = getRequiredConfig("JMS_PORT").toInt(),
    queueManager = getRequiredConfig("JMS_QUEUE_MANAGER"),
    username = getRequiredConfig("JMS_USERNAME"),
    password = getRequiredConfig("JMS_PASSWORD"),
)
