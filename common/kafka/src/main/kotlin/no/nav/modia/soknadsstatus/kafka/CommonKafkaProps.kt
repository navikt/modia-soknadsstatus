package no.nav.modia.soknadsstatus.kafka

import no.nav.modia.soknadsstatus.AppMode
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.errors.DefaultProductionExceptionHandler
import org.apache.kafka.streams.errors.DeserializationExceptionHandler
import java.util.*

private fun aivenSecurityProps(
    props: Properties,
    kafkaEnvironment: KafkaSecurityConfig,
) {
    props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SSL"
    props[SaslConfigs.SASL_MECHANISM] = "PLAIN"
    props[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = kafkaEnvironment.aivenBootstrapServers
    props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = kafkaEnvironment.aivenSecurityProtocol
    props[SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG] = ""
    props[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG] = "jks"
    props[SslConfigs.SSL_KEYSTORE_TYPE_CONFIG] = "PKCS12"
    props[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = kafkaEnvironment.aivenTruststoreLocation
    props[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = kafkaEnvironment.aivenCredstorePassword
    props[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] = kafkaEnvironment.aivenKeystoreLocation
    props[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = kafkaEnvironment.aivenCredstorePassword
    props[SslConfigs.SSL_KEY_PASSWORD_CONFIG] = kafkaEnvironment.aivenCredstorePassword
}

fun <TARGET_TYPE> commonStreamsConfig(
    props: Properties,
    appConfig: AppEnv,
    valueSerde: Serde<TARGET_TYPE>,
    deserializationExceptionHandler: DeserializationExceptionHandler,
    deadLetterQueueProducer: DeadLetterQueueProducer?
) {
    props[StreamsConfig.APPLICATION_ID_CONFIG] = appConfig.appName
    props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = appConfig.brokerUrl
    props[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.StringSerde().javaClass
   props[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = valueSerde::class.java
    props[StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG] =
        deserializationExceptionHandler::class.java
    props[StreamsConfig.DEFAULT_PRODUCTION_EXCEPTION_HANDLER_CLASS_CONFIG] =
        DefaultProductionExceptionHandler::class.java
    if (deadLetterQueueProducer != null) props["dlqProducer"] = deadLetterQueueProducer
    if (appConfig.appMode == AppMode.NAIS) {
        aivenSecurityProps(
            props,
            KafkaSecurityConfig()
        )
    }
}

fun commonProducerConfig(props: Properties, appConfig: AppEnv) {
    props[ProducerConfig.ACKS_CONFIG] = "all"
    props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = appConfig.brokerUrl
    props[ProducerConfig.CLIENT_ID_CONFIG] = "${appConfig.appName}-producer"
    if (appConfig.appMode == AppMode.NAIS) {
        aivenSecurityProps(
            props,
            KafkaSecurityConfig()
        )
    }
}

fun commonConsumerConfig(props: Properties, appConfig: AppEnv, valueDeserializer: Deserializer<*>) {
    props[ConsumerConfig.GROUP_ID_CONFIG] = "${appConfig.appName}-consumer-group-new"
    props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = appConfig.brokerUrl
    props[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "1"
    props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
    props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "false"
    props[ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG] = "" + (10 * 1024 * 1024)
    props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = valueDeserializer
    props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_DOC] = valueDeserializer
    if (appConfig.appMode == AppMode.NAIS) {
        aivenSecurityProps(
            props,
            KafkaSecurityConfig()
        )
    }
}
