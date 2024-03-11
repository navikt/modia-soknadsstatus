package no.nav.modia.soknadsstatus.kafka

import no.nav.modia.soknadsstatus.AppMode
import no.nav.personoversikt.common.utils.EnvUtils
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

data class AppEnv(
    val appName: String = EnvUtils.getRequiredConfig("APP_NAME"),
    val appMode: AppMode = AppMode(EnvUtils.getRequiredConfig("APP_MODE", mapOf("APP_MODE" to "NAIS"))),
    val appCluster: AppCluster =
        AppCluster(
            EnvUtils.getRequiredConfig(
                ("NAIS_CLUSTER_NAME"),
                mapOf("NAIS_CLUSTER_NAME" to "prod-gcp"),
            ),
        ),
    val appVersion: String = EnvUtils.getRequiredConfig("APP_VERSION"),
    val brokerUrls: String = EnvUtils.getRequiredConfig("KAFKA_BROKERS"),
    val sourceTopic: String? = EnvUtils.getConfig("KAFKA_SOURCE_TOPIC"),
    val sourceBehandlingTopic: String? = EnvUtils.getConfig("KAFKA_SOURCE_BEHANDLING_TOPIC"),
    val targetTopic: String? = EnvUtils.getConfig("KAFKA_TARGET_TOPIC"),
    val deadLetterQueueTopic: String? = EnvUtils.getConfig("KAFKA_DEAD_LETTER_QUEUE_TOPIC"),
    val deadLetterQueueBehandlingTopic: String? = EnvUtils.getConfig("KAFKA_DEAD_LETTER_QUEUE_BEHANDLING_TOPIC"),
    val deadLetterQueueConsumerPollIntervalMs: Double =
        EnvUtils
            .getConfig("KAFKA_DEAD_LETTER_QUEUE_CONSUMER_POLL_INTERVAL_MS")
            ?.toDouble() ?: 1.minutes.toDouble(DurationUnit.MILLISECONDS),
    val deadLetterQueueExceptionRestartDelayMs: Double =
        EnvUtils
            .getConfig("KAFKA_DEAD_LETTER_QUEUE_EXCEPTION_DELAY")
            ?.toDouble() ?: 10.minutes.toDouble(DurationUnit.MILLISECONDS),
    val deadLetterQueueSkipTableName: String? = EnvUtils.getConfig("KAFKA_DEAD_LETTER_QUEUE_SKIP_TABLE_NAME"),
    val deadLetterQueueMetricsGaugeName: String? = EnvUtils.getConfig("KAFKA_DEAD_LETTER_QUEUE_METRICS_GAUGE_NAME"),
    val slackWebHookUrl: String? = EnvUtils.getConfig("SLACK_WEBHOOK_URL"),
)
