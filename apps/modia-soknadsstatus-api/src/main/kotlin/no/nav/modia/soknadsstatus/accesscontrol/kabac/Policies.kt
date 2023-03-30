package no.nav.modia.soknadsstatus.accesscontrol.kabac

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EksternBrukerId
import no.nav.common.types.identer.Fnr
import no.nav.modia.soknadsstatus.accesscontrol.PolicyWithAttributes
import no.nav.modia.soknadsstatus.accesscontrol.kabac.policies.TilgangTilBrukerPolicy
import no.nav.personoversikt.common.kabac.AttributeValue
import no.nav.personoversikt.common.kabac.Kabac

object Policies {
    @JvmStatic
    fun tilgangTilBruker(eksternBrukerId: EksternBrukerId) = TilgangTilBrukerPolicy.withAttributes(
        eksternBrukerId.toAttributeValue()
    )

    private fun Kabac.Policy.withAttributes(vararg attributes: AttributeValue<*>) = PolicyWithAttributes(this, attributes.toList())
    private fun EksternBrukerId.toAttributeValue() = when (this) {
        is Fnr -> CommonAttributes.FNR.withValue(this)
        is AktorId -> CommonAttributes.AKTOR_ID.withValue(this)
        else -> throw IllegalArgumentException("Unsupported EksternBrukerID")
    }
}
