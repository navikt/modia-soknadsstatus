package no.nav.modia.soknadsstatus

import no.nav.modia.soknadsstatus.kafka.AppEnv
import no.nav.modia.soknadsstatus.pdl.PdlEnv
import no.nav.modia.soknadsstatus.pdlpip.PdlPipApiEnv
import no.nav.modia.soknadsstatus.tilgangsmaskinen.TilgangsmaskinenEnv
import no.nav.modia.soknadsstatus.utils.DownstreamApi
import no.nav.modia.soknadsstatus.utils.parse
import no.nav.personoversikt.common.utils.EnvUtils

interface Env {
    companion object {
        operator fun invoke(): Env {
            val kafkaApp = AppEnv()
            if (kafkaApp.appMode != AppMode.NAIS) {
                MockData.setUpMocks()
            }
            return EnvImpl(kafkaApp = kafkaApp)
        }
    }

    val appVersion: String
    val kafkaApp: AppEnv
    val hendelseConsumerEnv: HendelseConsumerEnv
    val azureAdConfiguration: AzureAdConfiguration
    val datasourceConfiguration: DatasourceConfiguration
    val pdlPipEnv: PdlPipApiEnv
    val pdlEnv: PdlEnv
    val pdlEnvQ1: PdlEnv
    val tilgangsmaskinenEnv: TilgangsmaskinenEnv

    val electorPath: String?
}

data class EnvImpl(
    override val kafkaApp: AppEnv = AppEnv(),
    override val appVersion: String = EnvUtils.getRequiredConfig("APP_VERSION"),
    override val hendelseConsumerEnv: HendelseConsumerEnv = HendelseConsumerEnv(),
    override val datasourceConfiguration: DatasourceConfiguration = DatasourceConfiguration(DatasourceEnv(kafkaApp.appName)),
    override val azureAdConfiguration: AzureAdConfiguration = AzureAdConfiguration.load(appMode = kafkaApp.appMode),
    override val pdlEnv: PdlEnv =
        PdlEnv(
            url = EnvUtils.getRequiredConfig("PDL_API_URL"),
            scope = EnvUtils.getRequiredConfig("PDL_SCOPE"),
        ),
    override val pdlEnvQ1: PdlEnv =
        PdlEnv(
            url = EnvUtils.getRequiredConfig("PDL_API_URL_Q1"),
            scope = EnvUtils.getRequiredConfig("PDL_SCOPE_Q1"),
        ),
    override val pdlPipEnv: PdlPipApiEnv =
        PdlPipApiEnv(
            url = EnvUtils.getRequiredConfig("PDL_PIP_API_URL"),
            scope = DownstreamApi.parse(EnvUtils.getRequiredConfig("PDL_PIP_SCOPE")),
        ),
    override val tilgangsmaskinenEnv: TilgangsmaskinenEnv =
        TilgangsmaskinenEnv(
            url = EnvUtils.getRequiredConfig("TILGANGSMASKINEN_URL"),
            scope = DownstreamApi.parse(EnvUtils.getRequiredConfig("TILGANGSMASKINEN_SCOPE")),
        ),
    override val electorPath: String? = EnvUtils.getConfig("ELECTOR_PATH"),
) : Env

data class HendelseConsumerEnv(
    val pollDurationMs: Double = EnvUtils.getRequiredConfig("HENDELSE_CONSUMER_POLL_DURATION_MS").toDouble(),
    val exceptionRestartDelayMs: Double = EnvUtils.getRequiredConfig("HENDELSE_CONSUMER_RESTART_DELAY_MS").toDouble(),
)
