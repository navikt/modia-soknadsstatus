package no.nav.modia.soknadsstatus

import io.ktor.http.*
import no.nav.modia.soknadsstatus.accesscontrol.AccessControlConfig
import no.nav.modia.soknadsstatus.accesscontrol.kabac.Policies
import no.nav.modia.soknadsstatus.ansatt.AnsattConfig
import no.nav.modia.soknadsstatus.axsys.AxsysConfig
import no.nav.modia.soknadsstatus.azure.AzureADServiceImpl
import no.nav.modia.soknadsstatus.kafka.DeadLetterMessageRepository
import no.nav.modia.soknadsstatus.kafka.DeadLetterMessageSkipService
import no.nav.modia.soknadsstatus.kafka.DeadLetterMessageSkipServiceImpl
import no.nav.modia.soknadsstatus.kafka.DeadLetterQueueProducer
import no.nav.modia.soknadsstatus.norg.NorgConfig
import no.nav.modia.soknadsstatus.pdl.PdlConfig
import no.nav.modia.soknadsstatus.pdl.PdlOppslagService
import no.nav.modia.soknadsstatus.skjermedepersoner.SkjermedePersonerConfig
import no.nav.modia.soknadsstatus.utils.bindTo

interface Services {
    val policies: Policies
    val pdl: PdlOppslagService
    val soknadsstatusService: SoknadsstatusService
    val accessControl: AccessControlConfig
    val dlqProducer: DeadLetterQueueProducer
    val dlSkipService: DeadLetterMessageSkipService

    companion object {
        fun factory(env: Env, configuration: Configuration): Services {
            val pdl = PdlConfig.factory(
                env.kafkaApp.appMode,
                env.pdlEnv,
                configuration.oboTokenClient,
                configuration.machineToMachineTokenClient
            )
            val soknadsstatusService = SoknadsstatusServiceImpl(pdl, configuration.repository)
            val norgApi = NorgConfig.factory(env.kafkaApp.appMode, env.norgEnv, env.kafkaApp.appName)
            val skjermedePersonerApi = SkjermedePersonerConfig.factory(
                env.kafkaApp.appMode,
                env.skjermedePersonerEnv,
                configuration.machineToMachineTokenClient
            )
            val axsysApi =
                AxsysConfig.factory(env.kafkaApp.appMode, env.axsysEnv, configuration.machineToMachineTokenClient)
            val azureADService = AzureADServiceImpl(
                graphUrl = Url(env.msGraphEnv.url),
                tokenClient = configuration.machineToMachineTokenClient.bindTo(
                    env.msGraphEnv.scope
                )
            )
            val ansattService =
                AnsattConfig.factory(
                    env.kafkaApp.appMode,
                    axsysApi,
                    azureADService,
                    env.sensitiveTilgangsRoller,
                    env.geografiskeTilgangsRoller
                )
            val accessControl = AccessControlConfig(
                pdl = pdl,
                skjermingApi = skjermedePersonerApi,
                norg = norgApi,
                ansattService = ansattService
            )
            val dlqProducer =
                DeadLetterQueueProducer(env.kafkaApp)
            val dlSkipService = DeadLetterMessageSkipServiceImpl(
                DeadLetterMessageRepository(
                    requireNotNull(env.kafkaApp.deadLetterQueueSkipTableName),
                    env.datasourceConfiguration.datasource
                )
            )

            return object : Services {
                override val policies: Policies = Policies(env.sensitiveTilgangsRoller, env.geografiskeTilgangsRoller)
                override val pdl = pdl
                override val soknadsstatusService = soknadsstatusService
                override val accessControl = accessControl
                override val dlSkipService = dlSkipService
                override val dlqProducer = dlqProducer
            }
        }
    }
}
