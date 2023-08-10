package no.nav.modia.soknadsstatus

import no.nav.modia.soknadsstatus.ansatt.GeografiskeTilgangsRoller
import no.nav.modia.soknadsstatus.ansatt.SensitiveTilgangsRoller
import no.nav.modia.soknadsstatus.axsys.AxsysEnv
import no.nav.modia.soknadsstatus.azure.MsGraphEnv
import no.nav.modia.soknadsstatus.kafka.AppEnv
import no.nav.modia.soknadsstatus.norg.NorgEnv
import no.nav.modia.soknadsstatus.pdl.PdlEnv
import no.nav.modia.soknadsstatus.skjermedepersoner.SkjermedePersonerEnv
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
    val azureAdConfiguration: AzureAdConfiguration
    val datasourceConfiguration: DatasourceConfiguration
    val axsysEnv: AxsysEnv
    val norgEnv: NorgEnv
    val skjermedePersonerEnv: SkjermedePersonerEnv
    val pdlEnv: PdlEnv
    val sensitiveTilgangsRoller: SensitiveTilgangsRoller
    val geografiskeTilgangsRoller: GeografiskeTilgangsRoller
    val msGraphEnv: MsGraphEnv
}

data class EnvImpl(
    override val kafkaApp: AppEnv = AppEnv(),
    override val appVersion: String = EnvUtils.getRequiredConfig("APP_VERSION"),
    override val datasourceConfiguration: DatasourceConfiguration = DatasourceConfiguration(DatasourceEnv(kafkaApp.appName)),
    override val azureAdConfiguration: AzureAdConfiguration = AzureAdConfiguration.load(appMode = kafkaApp.appMode),
    override val pdlEnv: PdlEnv = PdlEnv(
        url = EnvUtils.getRequiredConfig("PDL_API_URL"),
        scope = EnvUtils.getRequiredConfig("PDL_SCOPE"),
    ),
    override val axsysEnv: AxsysEnv = AxsysEnv(
        scope = EnvUtils.getRequiredConfig("AXSYS_SCOPE"),
        url = EnvUtils.getRequiredConfig("AXSYS_URL"),
    ),
    override val norgEnv: NorgEnv = NorgEnv(url = EnvUtils.getRequiredConfig("NORG2_URL")),
    override val skjermedePersonerEnv: SkjermedePersonerEnv = SkjermedePersonerEnv(
        url = EnvUtils.getRequiredConfig("SKJERMEDE_PERSONER_PIP_URL"),
        scope = DownstreamApi.parse(EnvUtils.getRequiredConfig("SKJERMEDE_PERSONER_SCOPE")),
    ),
    override val sensitiveTilgangsRoller: SensitiveTilgangsRoller = SensitiveTilgangsRoller(kafkaApp.appCluster),
    override val geografiskeTilgangsRoller: GeografiskeTilgangsRoller = GeografiskeTilgangsRoller(kafkaApp.appCluster),
    override val msGraphEnv: MsGraphEnv = MsGraphEnv(
        url = EnvUtils.getRequiredConfig("MS_GRAPH_URL"),
        scope = EnvUtils.getRequiredConfig("MS_GRAPH_SCOPE"),
        ),
) : Env
