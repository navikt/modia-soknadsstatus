package no.nav.modia.soknadsstatus.accesscontrol.kabac.providers

import kotlinx.coroutines.runBlocking
import no.nav.common.types.identer.EnhetId
import no.nav.modia.soknadsstatus.ansatt.AnsattService
import no.nav.modia.soknadsstatus.removeBearerFromToken
import no.nav.personoversikt.common.kabac.Kabac
import no.nav.personoversikt.common.kabac.Kabac.EvaluationContext
import no.nav.personoversikt.common.kabac.utils.Key

class VeiledersEnheterPip(
    private val ansattService: AnsattService,
) : Kabac.PolicyInformationPoint<List<EnhetId>> {
    override val key = Companion.key

    companion object : Kabac.AttributeKey<List<EnhetId>> {
        override val key = Key<List<EnhetId>>(VeiledersEnheterPip::class.java.simpleName)
    }

    override fun provide(ctx: EvaluationContext): List<EnhetId> {
        val subject = ctx.getValue(AuthContextPip)
        val ident = ctx.getValue(NavIdentPip)
        return runBlocking {
            ansattService.hentEnhetsliste(subject.token.removeBearerFromToken(), ident)
        }
    }
}
