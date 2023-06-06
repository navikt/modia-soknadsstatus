package no.nav.modia.soknadsstatus.kafka

import no.nav.modia.soknadsstatus.AppMode
import no.nav.personoversikt.common.utils.EnvUtils
data class AppEnv(
    val appName: String = EnvUtils.getRequiredConfig("APP_NAME"),
    val appMode: AppMode = AppMode(EnvUtils.getRequiredConfig("APP_MODE", mapOf("APP_MODE" to "NAIS"))),
    val appVersion: String = EnvUtils.getRequiredConfig("APP_VERSION"),
    val brokerUrls: String = EnvUtils.getRequiredConfig("KAFKA_BROKERS"),
    val sourceTopic: String? = EnvUtils.getConfig("KAFKA_SOURCE_TOPIC"),
    val targetTopic: String? = EnvUtils.getConfig("KAFKA_TARGET_TOPIC"),
    val deadLetterQueueTopic: String? = EnvUtils.getConfig("KAFKA_DEAD_LETTER_QUEUE_TOPIC"),
    val deadLetterQueueConsumerPollIntervalMs: Double = EnvUtils.getConfig("KAFKA_DEAD_LETTER_QUEUE_CONSUMER_POLL_INTERVAL_MS")?.toDouble() ?: 60000.0,
    val deadLetterQueueSkipTableName: String? = EnvUtils.getConfig("KAFKA_DEAD_LETTER_QUEUE_SKIP_TABLE_NAME"),
    val deadLetterQueueMetricsGaugeName: String? = EnvUtils.getConfig("KAFKA_DEAD_LETTER_QUEUE_METRICS_GAUGE_NAME")
)
