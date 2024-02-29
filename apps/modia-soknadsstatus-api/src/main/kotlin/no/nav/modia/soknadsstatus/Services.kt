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
import no.nav.modia.soknadsstatus.utils.LeaderElectionService
import no.nav.modia.soknadsstatus.utils.LeaderElectionServiceImpl
import no.nav.modia.soknadsstatus.utils.bindTo

interface Services {
    val policies: Policies
    val pdl: PdlOppslagService
    val pdlQ1: PdlOppslagService
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
            val pdl =
                PdlConfig.factory(
                    env.kafkaApp.appMode,
                    env.pdlEnv,
                    configuration.oboTokenClient,
                    configuration.machineToMachineTokenClient,
                )
            val pdlQ1 =
                PdlConfig.factory(
                    env.kafkaApp.appMode,
                    env.pdlEnvQ1,
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
                    pdlOppslagServiceQ1 = pdlQ1,
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
                override val pdlQ1 = pdlQ1
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
