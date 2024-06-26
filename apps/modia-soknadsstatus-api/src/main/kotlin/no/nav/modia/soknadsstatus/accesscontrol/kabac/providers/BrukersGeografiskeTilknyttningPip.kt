package no.nav.modia.soknadsstatus.accesscontrol.kabac.providers

import kotlinx.coroutines.runBlocking
import no.nav.modia.soknadsstatus.pdl.PdlOppslagService
import no.nav.personoversikt.common.kabac.Kabac
import no.nav.personoversikt.common.kabac.Kabac.EvaluationContext
import no.nav.personoversikt.common.kabac.utils.Key

class BrukersGeografiskeTilknyttningPip(
    private val pdl: PdlOppslagService,
) : Kabac.PolicyInformationPoint<String?> {
    override val key = Companion.key

    companion object : Kabac.AttributeKey<String?> {
        override val key = Key<String?>(BrukersGeografiskeTilknyttningPip::class.java.simpleName)
    }

    override fun provide(ctx: EvaluationContext): String? {
        val fnr = ctx.getValue(BrukersFnrPip.key).get()

        return runBlocking { pdl.hentGeografiskTilknytning(fnr) }
    }
}
