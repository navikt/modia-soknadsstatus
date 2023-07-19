package no.nav.modia.soknadsstatus.accesscontrol.kabac.providers

import kotlinx.coroutines.runBlocking
import no.nav.common.types.identer.Fnr
import no.nav.modia.soknadsstatus.accesscontrol.kabac.CommonAttributes
import no.nav.modia.soknadsstatus.pdl.PdlOppslagService
import no.nav.personoversikt.common.kabac.Kabac
import no.nav.personoversikt.common.kabac.Kabac.EvaluationContext

class BrukersFnrPip(private val pdl: PdlOppslagService) : Kabac.PolicyInformationPoint<Fnr> {
    override val key = Companion.key

    companion object : Kabac.AttributeKey<Fnr> {
        override val key = CommonAttributes.FNR
    }

    override fun provide(ctx: EvaluationContext): Fnr {
        val aktorId = ctx.getValue(CommonAttributes.AKTOR_ID)
        val prinicipal = requireNotNull(ctx.getValue(AuthContextPip)) {
            "Fikk ikke prinicipal fra authcontext"
        }
        val fnr = requireNotNull(runBlocking { pdl.hentFnr(prinicipal.token, aktorId.get())}) {
            "Fant ikke fnr for $aktorId"
        }
        return Fnr(fnr)
    }
}
