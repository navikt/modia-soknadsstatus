package no.nav.modia.soknadsstatus

import io.ktor.server.application.*
import io.ktor.server.engine.*
import kotlinx.coroutines.launch
import no.nav.modia.soknadsstatus.jms.JmsConsumer
import no.nav.modia.soknadsstatus.kafka.KafkaUtils
import no.nav.personoversikt.common.ktor.utils.Metrics
import no.nav.personoversikt.common.ktor.utils.Selftest
import org.apache.kafka.clients.producer.ProducerRecord
import javax.jms.TextMessage

fun Application.mqToKafkaModule() {
    val config = Configuration()
    val jmsConsumer = JmsConsumer(config.mqConfiguration)
    val kafkaProducer = KafkaUtils.createProducer(
        applicationId = config.appname,
        brokerUrl = config.kafkaConfiguration.brokerUrl
    )

    val transferJob = launch {
        jmsConsumer
            .subscribe(config.mqQueue)
            .collect { message ->
                when (message) {
                    is TextMessage -> {
                        log.info("Got MQMessage: ${message.text}")
                        kafkaProducer.send(ProducerRecord(config.kafkaTopic, message.text))
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
        appname = config.appname
        version = config.appversion
    }

    install(ShutDownUrl.ApplicationCallPlugin) {
        shutDownUrl = "/shutdown"
        exitCodeSupplier = {
            transferJob.cancel()
            0
        }
    }
}
