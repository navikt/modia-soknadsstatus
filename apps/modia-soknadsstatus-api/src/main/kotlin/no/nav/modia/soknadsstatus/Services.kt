package no.nav.modia.soknadsstatus

import no.nav.modia.soknadsstatus.accesscontrol.AccessControlConfig
import no.nav.modia.soknadsstatus.accesscontrol.kabac.Policies
import no.nav.modia.soknadsstatus.ansatt.AnsattConfig
import no.nav.modia.soknadsstatus.azure.AzureADServiceImpl
import no.nav.modia.soknadsstatus.azure.MsGraphConfig
import no.nav.modia.soknadsstatus.kafka.*
import no.nav.modia.soknadsstatus.norg.NorgConfig
import no.nav.modia.soknadsstatus.pdl.PdlConfig
import no.nav.modia.soknadsstatus.pdl.PdlOppslagService
import no.nav.modia.soknadsstatus.pdlpip.PdlPipConfig
import no.nav.modia.soknadsstatus.service.*
import no.nav.modia.soknadsstatus.skjermedepersoner.SkjermedePersonerConfig
import no.nav.modia.soknadsstatus.utils.LeaderElectionService
import no.nav.modia.soknadsstatus.utils.LeaderElectionServiceImpl
import no.nav.modia.soknadsstatus.utils.bindTo

interface Services {
    val policies: Policies
    val pdl: PdlOppslagService
    val pdlMigrering: PdlOppslagService
    val accessControl: AccessControlConfig
    val dlqProducer: DeadLetterQueueProducer
    val dlSkipService: DeadLetterMessageSkipService
    val behandlingEierService: BehandlingEierService
    val behandlingService: BehandlingService
    val hendelseService: HendelseService
    val hendelseEierService: HendelseEierService
    val leaderElectionService: LeaderElectionService

    companion object {
        fun factory(
            env: Env,
            configuration: Configuration,
        ): Services {
            val norgApi = NorgConfig.factory(env.kafkaApp.appMode, env.norgEnv, env.kafkaApp.appName)
            val skjermedePersonerApi =
                SkjermedePersonerConfig.factory(
                    env.kafkaApp.appMode,
                    env.skjermedePersonerEnv,
                    configuration.machineToMachineTokenClient,
                )
            val pdlPipApi = PdlPipConfig.factory(env.kafkaApp.appMode, env.pdlPipEnv, configuration.machineToMachineTokenClient)
            val pdl =
                PdlConfig.factory(
                    env.kafkaApp.appMode,
                    env.pdlEnv,
                    configuration.oboTokenClient,
                    configuration.machineToMachineTokenClient,
                    pdlPipApi,
                )
            val pdlMigrering =
                PdlConfig.factory(
                    env.kafkaApp.appMode,
                    env.pdlEnvQ1,
                    configuration.oboTokenClient,
                    configuration.machineToMachineTokenClient,
                    pdlPipApi,
                )
            val msGraphClient =
                MsGraphConfig.factory(env.kafkaApp.appMode, env.msGraphEnv)
            val azureADService =
                AzureADServiceImpl(
                    msGraphClient = msGraphClient,
                    tokenClient =
                        configuration.oboTokenClient.bindTo(
                            env.msGraphEnv.scope,
                        ),
                )
            val ansattService =
                AnsattConfig.factory(
                    env.kafkaApp.appMode,
                    azureADService,
                )
            val accessControl =
                AccessControlConfig(
                    pdl = pdl,
                    skjermingApi = skjermedePersonerApi,
                    norg = norgApi,
                    ansattService = ansattService,
                )
            val dlqProducer =
                DeadLetterQueueProducerImpl(env.kafkaApp)
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
            val leaderElectionService =
                LeaderElectionServiceImpl(
                    env.electorPath,
                )

            behandlingService.init(hendelseService)
            hendelseService.init(behandlingService)

            return object : Services {
                override val policies: Policies = Policies(env.sensitiveTilgangsRoller, env.geografiskeTilgangsRoller)
                override val pdl = pdl
                override val pdlMigrering = pdlMigrering
                override val accessControl = accessControl
                override val dlSkipService = dlSkipService
                override val dlqProducer = dlqProducer
                override val behandlingEierService = behandlingEierService
                override val behandlingService = behandlingService
                override val hendelseService = hendelseService
                override val hendelseEierService = hendelseEierService
                override val leaderElectionService = leaderElectionService
            }
        }
    }
}
