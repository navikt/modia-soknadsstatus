package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import io.ktor.server.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.nav.modia.soknadsstatus.jms.Jms
import no.nav.modia.soknadsstatus.jms.JmsConsumer
import no.nav.modia.soknadsstatus.kafka.AppEnv
import no.nav.modia.soknadsstatus.kafka.KafkaUtils
import no.nav.personoversikt.common.ktor.utils.Metrics
import no.nav.personoversikt.common.ktor.utils.Selftest
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serdes.StringSerde
import java.util.*
import javax.jms.TextMessage

fun Application.mqToKafkaModule() {
    val config = AppEnv()
    val mqConfig = MqConfig()

    Jms.SSLConfig(
        appMode = config.appMode,
        jmsKeyStorePath = mqConfig.config.jmsKeyStorePath,
        jmsPassword = mqConfig.config.jmsKeystorePassword
    ).injectSSLConfigIfProd()

    val jmsConsumer = JmsConsumer(mqConfig.config, config.appMode)

    val kafkaProducer = KafkaUtils.createProducer(
        config,
        StringSerde()
    )

    val transferJob = GlobalScope.launch(Dispatchers.Unbounded) {
        jmsConsumer
            .subscribe(mqConfig.mqQueue)
            .collect { message ->
                when (message) {
                    is TextMessage -> {
                        kafkaProducer.send(
                            ProducerRecord(
                                requireNotNull(config.targetTopic),
                                UUID.randomUUID().toString(),
                                message.text
                            )
                        )
                    }

                    else -> {
                        log.error("Message from MQ was not TextMessage, but: ${message::class.java.simpleName}")
                    }
                }
                message.acknowledge()
            }
    }

    install(Metrics.Plugin)
    install(Selftest.Plugin) {
        appname = config.appName
        version = config.appVersion
    }

    install(ShutDownUrl.ApplicationCallPlugin) {
        shutDownUrl = "/shutdown"
        exitCodeSupplier = {
            transferJob.cancel()
            0
        }
    }
}
