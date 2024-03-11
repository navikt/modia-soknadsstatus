package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import no.nav.modia.soknadsstatus.kafka.DeadLetterQueueConsumer

class BehandlingDeadLetterConfig {
    var deadLetterQueueConsumer: DeadLetterQueueConsumer? = null
}

class DeadLetterQueueBehandlingConsumerPlugin :
    Plugin<Pipeline<*, ApplicationCall>, BehandlingDeadLetterConfig, DeadLetterQueueBehandlingConsumerPlugin> {
    private var consumer: DeadLetterQueueConsumer? = null

    override val key: AttributeKey<DeadLetterQueueBehandlingConsumerPlugin> = AttributeKey("dead-letter-behandling-consumer")

    override fun install(
        pipeline: Pipeline<*, ApplicationCall>,
        configure: BehandlingDeadLetterConfig.() -> Unit,
    ): DeadLetterQueueBehandlingConsumerPlugin {
        val configuration = BehandlingDeadLetterConfig()
        configuration.configure()

        consumer = configuration.deadLetterQueueConsumer
        consumer?.start()

        return this
    }
}
