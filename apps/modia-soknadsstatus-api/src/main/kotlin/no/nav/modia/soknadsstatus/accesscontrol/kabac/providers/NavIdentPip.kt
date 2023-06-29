package no.nav.modia.soknadsstatus.accesscontrol.kabac.providers

import no.nav.common.types.identer.NavIdent
import no.nav.personoversikt.common.kabac.Kabac
import no.nav.personoversikt.common.kabac.Kabac.EvaluationContext
import no.nav.personoversikt.common.kabac.utils.Key

object NavIdentPip : Kabac.PolicyInformationPoint<NavIdent> {
    override val key = Key<NavIdent>(AzureObjectIdPip)

    override fun provide(ctx: EvaluationContext): NavIdent {
        val principal = requireNotNull(ctx.getValue(AuthContextPip)) {
            "Fikk ikke prinicipal fra authcontext"
        }
        return NavIdent(principal.subject)
    }
}
