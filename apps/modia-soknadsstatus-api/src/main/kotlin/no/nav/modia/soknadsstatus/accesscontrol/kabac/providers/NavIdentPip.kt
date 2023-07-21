package no.nav.modia.soknadsstatus.accesscontrol.kabac.providers

import no.nav.common.auth.Constants
import no.nav.common.auth.utils.IdentUtils
import no.nav.common.types.identer.NavIdent
import no.nav.personoversikt.common.kabac.Kabac
import no.nav.personoversikt.common.kabac.Kabac.EvaluationContext
import no.nav.personoversikt.common.kabac.utils.Key

object NavIdentPip : Kabac.PolicyInformationPoint<NavIdent> {
    override val key = Key<NavIdent>(NavIdentPip)

    override fun provide(ctx: EvaluationContext): NavIdent {
        val principal = ctx.getValue(AuthContextPip)

        val navIdentString =
            requireNotNull(
                principal.payload.getClaim(Constants.AAD_NAV_IDENT_CLAIM).asString() ?: principal.subject
            ) {
                "Fikk ikke NavIdent fra principal"
            }

        if (!IdentUtils.erGydligNavIdent(navIdentString)) {
            throw IllegalArgumentException("Ingen gyldig nav ident funnet på token")
        }

        return NavIdent(navIdentString)
    }
}
