package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import no.nav.modia.soknadsstatus.kafka.DeadLetterQueueConsumer

class DeadLetterConfig {
    var deadLetterQueueConsumer: DeadLetterQueueConsumer? = null
}

class DeadLetterQueueConsumerPlugin : Plugin<Pipeline<*, ApplicationCall>, DeadLetterConfig, DeadLetterQueueConsumerPlugin> {
    private var consumer: DeadLetterQueueConsumer? = null

    override val key: AttributeKey<DeadLetterQueueConsumerPlugin> = AttributeKey("dead-letter-consumer")

    override fun install(
        pipeline: Pipeline<*, ApplicationCall>,
        configure: DeadLetterConfig.() -> Unit
    ): DeadLetterQueueConsumerPlugin {
        val configuration = DeadLetterConfig()
        configuration.configure()

        consumer = configuration.deadLetterQueueConsumer
        consumer?.start()

        return this
    }
}
