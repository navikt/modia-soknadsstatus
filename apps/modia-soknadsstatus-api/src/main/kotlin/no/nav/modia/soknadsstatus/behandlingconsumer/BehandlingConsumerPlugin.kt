package no.nav.modia.soknadsstatus.behandlingconsumer

import io.ktor.server.application.*
import io.ktor.util.*
import io.ktor.util.pipeline.*

class BehandlingConfig {
    var behandlingConsumer: BehandlingConsumer? = null
}

class BehandlingConsumerPlugin : Plugin<Pipeline<*, ApplicationCall>, BehandlingConfig, BehandlingConsumerPlugin> {
    private var consumer: BehandlingConsumer? = null

    override val key: AttributeKey<BehandlingConsumerPlugin> = AttributeKey("behandling-consumer")

    override fun install(
        pipeline: Pipeline<*, ApplicationCall>,
        configure: BehandlingConfig.() -> Unit,
    ): BehandlingConsumerPlugin {
        val configuration = BehandlingConfig()
        configuration.configure()

        consumer = configuration.behandlingConsumer
        requireNotNull(consumer).start()

        return this
    }
}
