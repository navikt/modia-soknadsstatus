package no.nav.modia.soknadsstatus

import no.nav.modia.soknadsstatus.accesscontrol.AccessControlConfig
import no.nav.modia.soknadsstatus.accesscontrol.kabac.Policies
import no.nav.modia.soknadsstatus.kafka.*
import no.nav.modia.soknadsstatus.pdl.PdlConfig
import no.nav.modia.soknadsstatus.pdl.PdlOppslagService
import no.nav.modia.soknadsstatus.pdlpip.PdlPipConfig
import no.nav.modia.soknadsstatus.service.*
import no.nav.modia.soknadsstatus.tilgangsmaskinen.TilgangsmaskinenConfig
import no.nav.modia.soknadsstatus.utils.LeaderElectionService
import no.nav.modia.soknadsstatus.utils.LeaderElectionServiceImpl

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
            val tilgangsmaskinen =
                TilgangsmaskinenConfig.factory(
                    appMode = env.kafkaApp.appMode,
                    env = env.tilgangsmaskinenEnv,
                    tokenProvider =
                        configuration.machineToMachineTokenClient,
                )
            val accessControl =
                AccessControlConfig(
                    tilgangsmaskinen = tilgangsmaskinen,
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
                override val policies: Policies = Policies()
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
