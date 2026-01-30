package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import io.ktor.server.engine.*
import jakarta.jms.TextMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.nav.modia.soknadsstatus.jms.Jms
import no.nav.modia.soknadsstatus.jms.JmsConsumer
import no.nav.modia.soknadsstatus.kafka.AppEnv
import no.nav.modia.soknadsstatus.kafka.KafkaUtils
import no.nav.modia.soknadsstatus.kafka.SlackClient
import no.nav.personoversikt.common.ktor.utils.Metrics
import no.nav.personoversikt.common.ktor.utils.Selftest
import no.nav.personoversikt.common.logging.TjenestekallLogg
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.*

fun Application.mqToKafkaModule() {
    val config = AppEnv()
    val mqConfig = MqConfig()
    val slackClient = config.slackWebHookUrl?.let { SlackClient(it) }

    Jms
        .SSLConfig(
            appMode = config.appMode,
            jmsKeyStorePath = mqConfig.config.jmsKeyStorePath,
            jmsPassword = mqConfig.config.jmsKeystorePassword,
        ).injectSSLConfigIfProd()

    val jmsConsumer = JmsConsumer(mqConfig.config, config.appMode)

    val kafkaProducer =
        KafkaUtils.createProducer(
            config,
        )

    val transferJob =
        GlobalScope.launch(Dispatchers.Unbounded) {
            try {
                jmsConsumer
                    .subscribe(mqConfig.mqQueue) { message ->
                        when (message) {
                            is TextMessage -> {
                                try {
                                    kafkaProducer
                                        .send(
                                            ProducerRecord(
                                                requireNotNull(config.targetTopic),
                                                UUID.randomUUID().toString(),
                                                message.text,
                                            ),
                                        ).get()
                                    message.acknowledge()
                                } catch (e: Exception) {
                                    TjenestekallLogg.error(
                                        header = "Klarte ikke å sende melding på kafka",
                                        fields = mapOf("message" to message),
                                        throwable = e,
                                    )
                                    slackClient?.postMessage("MqToKafkaModule: Klarte ikke å sende melding på kafka")
                                    throw e
                                }
                            }

                            else -> {
                                log.error("MqToKafkaModule: Message from MQ was not TextMessage, but: ${message::class.java.simpleName}")
                                slackClient?.postMessage(
                                    "MqToKafkaModule: Message from MQ was not TextMessage, but: ${message::class.java.simpleName}",
                                )
                                message.acknowledge()
                            }
                        }
                    }
            } catch (e: Exception) {
                TjenestekallLogg.error(
                    header = "MqToKafkaModule: Klarte ikke å koble til MQ:",
                    fields = mapOf(),
                    throwable = e,
                )
                throw e
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
