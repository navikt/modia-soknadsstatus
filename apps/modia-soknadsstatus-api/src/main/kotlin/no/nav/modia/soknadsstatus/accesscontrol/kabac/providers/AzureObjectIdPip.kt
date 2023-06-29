package no.nav.modia.soknadsstatus.accesscontrol.kabac.providers

import no.nav.common.types.identer.AzureObjectId
import no.nav.personoversikt.common.kabac.Kabac
import no.nav.personoversikt.common.kabac.utils.Key

object AzureObjectIdPip : Kabac.PolicyInformationPoint<AzureObjectId> {
    override val key = Key<AzureObjectId>(AzureObjectIdPip)

    override fun provide(ctx: Kabac.EvaluationContext): AzureObjectId {
        val principal = requireNotNull(ctx.getValue(AuthContextPip)) {
            "Fikk ikke prinicipal fra authcontext"
        }

        val oid = requireNotNull(principal.payload.getClaim("oid")) {
            "Fant ikke oid for bruker"
        }

        return AzureObjectId(oid.asString())
    }
}
