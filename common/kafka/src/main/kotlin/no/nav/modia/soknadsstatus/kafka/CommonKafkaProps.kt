package no.nav.modia.soknadsstatus.kafka

import no.nav.modia.soknadsstatus.AppMode
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.errors.DefaultProductionExceptionHandler
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

fun commonStreamsConfig(
    props: Properties,
    appConfig: AppEnv,
) {
    props[StreamsConfig.APPLICATION_ID_CONFIG] = "${appConfig.appName}-stream"
    props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = appConfig.brokerUrls
    props[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.StringSerde().javaClass
    props[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.StringSerde().javaClass
    props[StreamsConfig.DEFAULT_PRODUCTION_EXCEPTION_HANDLER_CLASS_CONFIG] =
        DefaultProductionExceptionHandler::class.java
    if (appConfig.appMode == AppMode.NAIS) {
        aivenSecurityProps(
            props,
            KafkaSecurityConfig(),
        )
    }
}

fun commonProducerConfig(
    props: Properties,
    appConfig: AppEnv,
) {
    props[ProducerConfig.ACKS_CONFIG] = "all"
    props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = appConfig.brokerUrls
    props[ProducerConfig.CLIENT_ID_CONFIG] = "${appConfig.appName}-producer"
    if (appConfig.appMode == AppMode.NAIS) {
        aivenSecurityProps(
            props,
            KafkaSecurityConfig(),
        )
    }
}

fun commonConsumerConfig(
    props: Properties,
    appConfig: AppEnv,
    consumerGroup: String,
    autoCommit: Boolean,
    pollRecords: Int,
) {
    props[ConsumerConfig.GROUP_ID_CONFIG] = consumerGroup
    props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = appConfig.brokerUrls
    props[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = pollRecords
    props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
    props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = autoCommit
    props[ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG] = "" + (10 * 1024 * 1024)
    props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = Serdes.StringSerde().deserializer().javaClass
    if (appConfig.appMode == AppMode.NAIS) {
        aivenSecurityProps(
            props,
            KafkaSecurityConfig(),
        )
    }
}
