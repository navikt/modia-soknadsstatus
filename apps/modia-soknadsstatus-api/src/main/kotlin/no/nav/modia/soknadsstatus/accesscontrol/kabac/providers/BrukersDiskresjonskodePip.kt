package no.nav.modia.soknadsstatus.accesscontrol.kabac.providers

import kotlinx.coroutines.runBlocking
import no.nav.api.generated.pdl.enums.AdressebeskyttelseGradering
import no.nav.api.generated.pdl.hentadressebeskyttelse.Adressebeskyttelse
import no.nav.modia.soknadsstatus.accesscontrol.kabac.CommonAttributes
import no.nav.modia.soknadsstatus.pdl.PdlOppslagService
import no.nav.personoversikt.common.kabac.Kabac
import no.nav.personoversikt.common.kabac.Kabac.EvaluationContext
import no.nav.personoversikt.common.kabac.utils.Key

class BrukersDiskresjonskodePip(
    private val pdl: PdlOppslagService,
) : Kabac.PolicyInformationPoint<BrukersDiskresjonskodePip.Kode?> {
    enum class Kode { KODE6, KODE7 }

    override val key = Companion.key

    companion object : Kabac.AttributeKey<Kode?> {
        override val key = Key<Kode?>(BrukersDiskresjonskodePip::class.java.simpleName)
    }

    override fun provide(ctx: EvaluationContext): Kode? {
        val fnr = ctx.getValue(CommonAttributes.FNR)
        val prinicipal = ctx.getValue(AuthContextPip)

        return runBlocking { pdl.hentAdresseBeskyttelse(prinicipal.token, fnr.get()).finnStrengesteKode() }
    }

    private fun List<Adressebeskyttelse>.finnStrengesteKode(): Kode? =
        this
            .mapNotNull {
                when (it.gradering) {
                    AdressebeskyttelseGradering.STRENGT_FORTROLIG, AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> Kode.KODE6
                    AdressebeskyttelseGradering.FORTROLIG -> Kode.KODE7
                    else -> null
                }
            }.minOrNull()
}
