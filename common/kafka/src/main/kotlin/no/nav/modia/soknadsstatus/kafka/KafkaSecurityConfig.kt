package no.nav.modia.soknadsstatus.kafka

import no.nav.personoversikt.common.utils.EnvUtils

data class KafkaSecurityConfig(
    val aivenBootstrapServers: String = EnvUtils.getRequiredConfig("KAFKA_BROKERS"),
    val aivenCredstorePassword: String = EnvUtils.getRequiredConfig("KAFKA_CREDSTORE_PASSWORD"),
    val aivenKeystoreLocation: String = EnvUtils.getRequiredConfig("KAFKA_AIVEN_KEYSTORE_LOCATION"),
    val aivenSecurityProtocol: String = EnvUtils.getRequiredConfig("KAFKA_AIVEN_SECURITY_PROTOCOL"),
    val aivenTruststoreLocation: String = EnvUtils.getRequiredConfig("KAFKA_AIVEN_TRUSTOSTORE_LOCATION"),
    val aivenSchemaRegistryUrl: String = EnvUtils.getRequiredConfig("KAFKA_SCHEMA_REGISTRY"),
    val aivenRegistryUser: String = EnvUtils.getRequiredConfig("KAFKA_SCHEMA_REGISTRY_USER"),
    val aivenRegistryPassword: String = EnvUtils.getRequiredConfig("KAFKA_SCHEMA_REGISTRY_PASSWORD"),
)
