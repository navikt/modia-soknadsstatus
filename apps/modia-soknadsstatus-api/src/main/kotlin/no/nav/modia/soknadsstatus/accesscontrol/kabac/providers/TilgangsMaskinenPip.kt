package no.nav.modia.soknadsstatus.accesscontrol.kabac.providers

import no.nav.modia.soknadsstatus.accesscontrol.kabac.CommonAttributes
import no.nav.modia.soknadsstatus.tilgangsmaskinen.Tilgangsmaskinen
import no.nav.personoversikt.common.kabac.Kabac
import no.nav.personoversikt.common.kabac.utils.Key

data class TilgangsMaskinResult(
    val tilgang: Boolean,
    val overridable: Boolean? = null,
    val begrunnelse: String? = null,
)

class TilgangsMaskinenPip(
    private val tilgangsmaskinen: Tilgangsmaskinen,
) : Kabac.PolicyInformationPoint<TilgangsMaskinResult?> {
    override val key = Companion.key

    companion object : Kabac.AttributeKey<TilgangsMaskinResult?> {
        override val key = Key<TilgangsMaskinResult?>(TilgangsMaskinenPip::class.java.simpleName)
    }

    override fun provide(ctx: Kabac.EvaluationContext): TilgangsMaskinResult? {
        val fnr = ctx.getValue(CommonAttributes.FNR)
        val ident = ctx.getValue(NavIdentPip)
        return tilgangsmaskinen.sjekkTilgang(ident, fnr)?.let {
            TilgangsMaskinResult(it.harTilgang, begrunnelse = "")
        }
    }
}
