package no.nav.modia.soknadsstatus

import no.nav.modia.soknadsstatus.accesscontrol.AccessControlConfig
import no.nav.modia.soknadsstatus.ansatt.AnsattConfig
import no.nav.modia.soknadsstatus.axsys.AxsysConfig
import no.nav.modia.soknadsstatus.ldap.LDAPContextProviderImpl
import no.nav.modia.soknadsstatus.ldap.LDAPServiceImpl
import no.nav.modia.soknadsstatus.nom.NomConfig
import no.nav.modia.soknadsstatus.norg.NorgConfig
import no.nav.modia.soknadsstatus.pdl.PdlConfig
import no.nav.modia.soknadsstatus.pdl.PdlOppslagService
import no.nav.modia.soknadsstatus.skjermedepersoner.SkjermedePersonerConfig

interface Services {
    val pdl: PdlOppslagService
    val soknadsstatusService: SoknadsstatusService
    val accessControl: AccessControlConfig

    companion object {
        fun factory(env: Env, configuration: Configuration): Services {
            val pdl = PdlConfig.factory(env.appMode, env.pdlEnv, configuration.oboTokenClient, configuration.machineToMachineTokenClient)
            val soknadsstatusService = SoknadsstatusServiceImpl(pdl, configuration.repository)
            val ldapApi = LDAPServiceImpl(LDAPContextProviderImpl(env.ldapEnv))
            val norgApi = NorgConfig.factory(env.appMode, env.norgEnv)
            val nomApi = NomConfig.factory(env.appMode, env.nomEnv, configuration.machineToMachineTokenClient)
            val skjermedePersonerApi = SkjermedePersonerConfig.factory(
                env.appMode,
                env.skjermedePersonerEnv,
                configuration.machineToMachineTokenClient
            )
            val axsysApi = AxsysConfig.factory(env.appMode, env.axsysEnv, configuration.machineToMachineTokenClient)
            val ansattService = AnsattConfig.factory(env.appMode, axsysApi, nomApi, ldapApi)
            val accessControl = AccessControlConfig(
                pdl = pdl,
                skjermingApi = skjermedePersonerApi,
                norg = norgApi,
                ansattService = ansattService
            )

            return object : Services {
                override val pdl = pdl
                override val soknadsstatusService = soknadsstatusService
                override val accessControl = accessControl
            }
        }
    }
}
