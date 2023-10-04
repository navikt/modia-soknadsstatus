package no.nav.modia.soknadsstatus

import io.ktor.http.*
import no.nav.modia.soknadsstatus.accesscontrol.AccessControlConfig
import no.nav.modia.soknadsstatus.accesscontrol.kabac.Policies
import no.nav.modia.soknadsstatus.ansatt.AnsattConfig
import no.nav.modia.soknadsstatus.axsys.AxsysConfig
import no.nav.modia.soknadsstatus.azure.AzureADServiceImpl
import no.nav.modia.soknadsstatus.kafka.*
import no.nav.modia.soknadsstatus.norg.NorgConfig
import no.nav.modia.soknadsstatus.pdl.PdlConfig
import no.nav.modia.soknadsstatus.pdl.PdlOppslagService
import no.nav.modia.soknadsstatus.service.*
import no.nav.modia.soknadsstatus.skjermedepersoner.SkjermedePersonerConfig
import no.nav.modia.soknadsstatus.utils.bindTo

interface Services {
    val policies: Policies
    val pdl: PdlOppslagService
    val accessControl: AccessControlConfig
    val dlqProducer: DeadLetterQueueProducer
    val dlSkipService: DeadLetterMessageSkipService
    val dlqMetricsGauge: DeadLetterQueueMetricsGauge
    val behandlingEierService: BehandlingEierService
    val behandlingService: BehandlingService
    val hendelseService: HendelseService
    val hendelseEierService: HendelseEierService

    companion object {
        fun factory(
            env: Env,
            configuration: Configuration,
        ): Services {
            val pdl =
                PdlConfig.factory(
                    env.kafkaApp.appMode,
                    env.pdlEnv,
                    configuration.oboTokenClient,
                    configuration.machineToMachineTokenClient,
                )
            val norgApi = NorgConfig.factory(env.kafkaApp.appMode, env.norgEnv, env.kafkaApp.appName)
            val skjermedePersonerApi =
                SkjermedePersonerConfig.factory(
                    env.kafkaApp.appMode,
                    env.skjermedePersonerEnv,
                    configuration.machineToMachineTokenClient,
                )
            val axsysApi =
                AxsysConfig.factory(env.kafkaApp.appMode, env.axsysEnv, configuration.machineToMachineTokenClient)
            val azureADService =
                AzureADServiceImpl(
                    graphUrl = Url(env.msGraphEnv.url),
                    tokenClient =
                        configuration.oboTokenClient.bindTo(
                            env.msGraphEnv.scope,
                        ),
                )
            val ansattService =
                AnsattConfig.factory(
                    env.kafkaApp.appMode,
                    axsysApi,
                    azureADService,
                    env.sensitiveTilgangsRoller,
                    env.geografiskeTilgangsRoller,
                )
            val accessControl =
                AccessControlConfig(
                    pdl = pdl,
                    skjermingApi = skjermedePersonerApi,
                    norg = norgApi,
                    ansattService = ansattService,
                )
            val dlqMetricsGauge =
                DeadLetterQueueMetricsGaugeImpl(requireNotNull(env.kafkaApp.deadLetterQueueMetricsGaugeName))
            val dlqProducer =
                DeadLetterQueueProducerImpl(env.kafkaApp, dlqMetricsGauge)
            val dlSkipService =
                DeadLetterMessageSkipServiceImpl(
                    DeadLetterMessageRepository(
                        requireNotNull(env.kafkaApp.deadLetterQueueSkipTableName),
                        env.datasourceConfiguration.datasource,
                    ),
                )
            val behandlingEierService = BehandlingEierServiceImpl(configuration.behandlingEiereRepository)
            val behandlingService = BehandlingServiceImpl(configuration.behandlingRepository, pdl)
            val hendelseEierService = HendelseEierServiceImpl(configuration.hendelseEierRepository)
            val hendelseService =
                HendelseServiceImpl(
                    pdlOppslagService = pdl,
                    hendelseRepository = configuration.hendelseRepository,
                    behandlingEierService = behandlingEierService,
                    hendelseEierService = hendelseEierService,
                )

            behandlingService.init(hendelseService)
            hendelseService.init(behandlingService)

            return object : Services {
                override val policies: Policies = Policies(env.sensitiveTilgangsRoller, env.geografiskeTilgangsRoller)
                override val pdl = pdl
                override val accessControl = accessControl
                override val dlSkipService = dlSkipService
                override val dlqProducer = dlqProducer
                override val dlqMetricsGauge = dlqMetricsGauge
                override val behandlingEierService = behandlingEierService
                override val behandlingService = behandlingService
                override val hendelseService = hendelseService
                override val hendelseEierService = hendelseEierService
            }
        }
    }
}
