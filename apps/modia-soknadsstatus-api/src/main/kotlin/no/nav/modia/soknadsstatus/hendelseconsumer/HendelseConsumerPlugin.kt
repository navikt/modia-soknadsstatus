package no.nav.modia.soknadsstatus.hendelseconsumer

import io.ktor.server.application.*
import io.ktor.util.*
import io.ktor.util.pipeline.*

class Config {
    var hendelseConsumer: HendelseConsumer? = null
}

class HendelseConsumerPlugin :
    Plugin<Pipeline<*, ApplicationCall>, Config, HendelseConsumerPlugin> {
    private var consumer: HendelseConsumer? = null

    override val key: AttributeKey<HendelseConsumerPlugin> = AttributeKey("hendelse-consumer")

    override fun install(
        pipeline: Pipeline<*, ApplicationCall>,
        configure: Config.() -> Unit,
    ): HendelseConsumerPlugin {
        val configuration = Config()
        configuration.configure()

        consumer = configuration.hendelseConsumer
        requireNotNull(consumer).start()

        return this
    }
}
