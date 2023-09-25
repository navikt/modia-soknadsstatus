package no.nav.modia.soknadsstatus.accesscontrol.kabac.providers

import kotlinx.coroutines.runBlocking
import no.nav.common.types.identer.AktorId
import no.nav.modia.soknadsstatus.accesscontrol.kabac.CommonAttributes
import no.nav.modia.soknadsstatus.pdl.PdlOppslagService
import no.nav.personoversikt.common.kabac.Kabac
import no.nav.personoversikt.common.kabac.Kabac.EvaluationContext

class BrukersAktorIdPip(private val pdl: PdlOppslagService) : Kabac.PolicyInformationPoint<AktorId> {
    override val key = Companion.key

    companion object : Kabac.AttributeKey<AktorId> {
        override val key = CommonAttributes.AKTOR_ID
    }

    override fun provide(ctx: EvaluationContext): AktorId {
        val fnr = ctx.getValue(CommonAttributes.FNR)
        val prinicipal = ctx.getValue(AuthContextPip)

        val aktorid = requireNotNull(runBlocking { pdl.hentAktorId(prinicipal.token, fnr.get()) }) {
            "Fant ikke aktor id for $fnr"
        }
        return AktorId(aktorid)
    }
}
