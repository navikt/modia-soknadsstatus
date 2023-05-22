package no.nav.modia.soknadsstatus.accesscontrol

import io.ktor.http.*
import io.ktor.server.auth.*
import no.nav.modia.soknadsstatus.HttpStatusException
import no.nav.modia.soknadsstatus.accesscontrol.kabac.providers.*
import no.nav.modia.soknadsstatus.accesscontrol.kabac.providers.BrukersFnrPip
import no.nav.modia.soknadsstatus.accesscontrol.kabac.providers.BrukersRegionEnhetPip
import no.nav.modia.soknadsstatus.accesscontrol.kabac.providers.NavIdentPip
import no.nav.modia.soknadsstatus.ansatt.AnsattService
import no.nav.modia.soknadsstatus.norg.NorgApi
import no.nav.modia.soknadsstatus.pdl.PdlOppslagService
import no.nav.modia.soknadsstatus.skjermedepersoner.SkjermedePersonerApi
import no.nav.personoversikt.common.kabac.Decision
import no.nav.personoversikt.common.kabac.impl.PolicyDecisionPointImpl
import no.nav.personoversikt.common.kabac.impl.PolicyEnforcementPointImpl
import no.nav.personoversikt.common.logging.Logging

class AccessControlConfig(
    private val pdl: PdlOppslagService,
    private val skjermingApi: SkjermedePersonerApi,
    private val norg: NorgApi,
    private val ansattService: AnsattService,
) {
    fun buildKabac(authenticationContext: AuthenticationContext): AccessControlKabac {
        val decisionPoint = PolicyDecisionPointImpl().apply {
            install(AuthContextPip(authenticationContext))
            install(NavIdentPip)
            install(BrukersFnrPip(pdl))
            install(BrukersAktorIdPip(pdl))
            install(BrukersDiskresjonskodePip(pdl))
            install(BrukersSkjermingPip(skjermingApi))
            install(BrukersEnhetPip(norg))
            install(BrukersGeografiskeTilknyttningPip(pdl))
            install(BrukersRegionEnhetPip(norg))
            install(VeiledersEnheterPip(ansattService))
            install(VeiledersRegionEnheterPip(norg))
            install(VeiledersRollerPip(ansattService))
        }
        val enforcementPoint = PolicyEnforcementPointImpl(
            bias = Decision.Type.DENY,
            policyDecisionPoint = decisionPoint
        )

        return AccessControlKabac(enforcementPoint) {
            Logging.secureLog.error(it)
            HttpStatusException(HttpStatusCode.Forbidden, it)
        }
    }
}