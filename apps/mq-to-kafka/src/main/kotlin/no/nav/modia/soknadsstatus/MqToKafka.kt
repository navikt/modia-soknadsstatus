package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import io.ktor.server.engine.*
import kotlinx.coroutines.launch
import no.nav.modia.soknadsstatus.jms.JmsConsumer
import no.nav.modia.soknadsstatus.kafka.AppEnv
import no.nav.modia.soknadsstatus.kafka.KafkaUtils
import no.nav.personoversikt.common.ktor.utils.Metrics
import no.nav.personoversikt.common.ktor.utils.Selftest
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serdes.StringSerde
import javax.jms.TextMessage

fun Application.mqToKafkaModule() {
    val config = AppEnv()
    val mqConfig = MqConfig()
    val jmsConsumer = JmsConsumer(mqConfig.config)

    val kafkaProducer = KafkaUtils.createProducer(
        config,
        StringSerde()
    )

    val transferJob = launch {
        jmsConsumer
            .subscribe(mqConfig.mqQueue)
            .collect { message ->
                when (message) {
                    is TextMessage -> {
                        log.info("Got MQMessage: ${message.text}")
                        kafkaProducer.send(
                            ProducerRecord(
                                requireNotNull(config.targetTopic),
                                message.jmsMessageID,
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
