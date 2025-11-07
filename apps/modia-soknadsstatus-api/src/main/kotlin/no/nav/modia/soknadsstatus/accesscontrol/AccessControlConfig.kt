package no.nav.modia.soknadsstatus.accesscontrol

import io.ktor.http.*
import io.ktor.server.auth.*
import no.nav.modia.soknadsstatus.HttpStatusException
import no.nav.modia.soknadsstatus.accesscontrol.kabac.providers.*
import no.nav.modia.soknadsstatus.tilgangsmaskinen.Tilgangsmaskinen
import no.nav.personoversikt.common.kabac.Decision
import no.nav.personoversikt.common.kabac.impl.PolicyDecisionPointImpl
import no.nav.personoversikt.common.kabac.impl.PolicyEnforcementPointImpl
import no.nav.personoversikt.common.logging.TjenestekallLogg

class AccessControlConfig(
    private val tilgangsmaskinen: Tilgangsmaskinen,
) {
    fun buildKabac(authenticationContext: AuthenticationContext): AccessControlKabac {
        val decisionPoint =
            PolicyDecisionPointImpl().apply {
                install(AuthContextPip(authenticationContext))
                install(TilgangsMaskinenPip(tilgangsmaskinen))
            }
        val enforcementPoint =
            PolicyEnforcementPointImpl(
                bias = Decision.Type.DENY,
                policyDecisionPoint = decisionPoint,
            )

        return AccessControlKabac(enforcementPoint) {
            TjenestekallLogg.info("KABAC exception: $it", fields = mapOf("error" to it))
            HttpStatusException(HttpStatusCode.Forbidden, it)
        }
    }
}
