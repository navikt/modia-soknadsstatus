package no.nav.modia.soknadsstatus.kafka

import no.nav.modia.soknadsstatus.AppMode
import no.nav.personoversikt.common.utils.EnvUtils

interface AppEnv {
    companion object {
        operator fun invoke(): AppEnv {
            return AppEnvImpl()
        }

        data class AppEnvImpl(
            override val appName: String = EnvUtils.getRequiredConfig("APP_NAME"),
            override val appMode: AppMode = AppMode(EnvUtils.getRequiredConfig("APP_MODE", mapOf("APP_MODE" to "NAIS"))),
            override val appVersion: String = EnvUtils.getRequiredConfig("APP_VERSION"),
            override val brokerUrl: String = EnvUtils.getRequiredConfig("KAFKA_BROKER_URL"),
            override val sourceTopic: String? = EnvUtils.getConfig("KAFKA_SOURCE_TOPIC"),
            override val targetTopic: String? = EnvUtils.getConfig("KAFKA_TARGET_TOPIC"),
            override val deadLetterQueueTopic: String? = EnvUtils.getConfig("KAFKA_DEAD_LETTER_QUEUE_TOPIC"),
            override val deadLetterQueueConsumerPollIntervalMs: Double = EnvUtils.getConfig("KAFKA_DEAD_LETTER_QUEUE_CONSUMER_POLL_INTERVAL_MS")?.toDouble() ?: 60000.0,
            override val deadLetterQueueSkipTableName: String? = EnvUtils.getConfig("KAFKA_DEAD_LETTER_QUEUE_SKIP_TABLE_NAME"),
            override val deadLetterQueueMetricsGaugeName: String? = EnvUtils.getConfig("KAFKA_DEAD_LETTER_QUEUE_METRICS_GAUGE_NAME")
        ) : AppEnv
    }

    val appName: String
    val appMode: AppMode
    val appVersion: String
    val brokerUrl: String
    val sourceTopic: String?
    val targetTopic: String?
    val deadLetterQueueTopic: String?
    val deadLetterQueueConsumerPollIntervalMs: Double
    val deadLetterQueueSkipTableName: String?
    val deadLetterQueueMetricsGaugeName: String?
}


