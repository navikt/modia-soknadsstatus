package no.nav.modia.soknadsstatus.accesscontrol.kabac.providers

import kotlinx.coroutines.runBlocking
import no.nav.modia.soknadsstatus.ansatt.AnsattService
import no.nav.modia.soknadsstatus.ansatt.RolleListe
import no.nav.personoversikt.common.kabac.Kabac
import no.nav.personoversikt.common.kabac.utils.Key

class VeiledersRollerPip(private val ansattService: AnsattService) : Kabac.PolicyInformationPoint<RolleListe> {
    override val key: Key<RolleListe> = Companion.key

    companion object {
        val key = Key<RolleListe>(VeiledersRollerPip::class.java.simpleName)
    }

    override fun provide(ctx: Kabac.EvaluationContext): RolleListe {
        val subject = ctx.getValue(AuthContextPip)
        val veilederNavIdent = ctx.getValue(NavIdentPip)

        return runBlocking {
            ansattService.hentVeiledersGeografiskeOgSensitiveRoller(subject.token, veilederNavIdent)
        }
    }
}
