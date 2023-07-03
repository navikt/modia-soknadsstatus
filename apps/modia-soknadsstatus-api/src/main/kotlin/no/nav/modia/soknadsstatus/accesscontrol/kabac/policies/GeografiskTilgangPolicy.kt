package no.nav.modia.soknadsstatus.accesscontrol.kabac.policies

import no.nav.common.types.identer.EnhetId
import no.nav.modia.soknadsstatus.accesscontrol.DenyCauseCode
import no.nav.modia.soknadsstatus.accesscontrol.kabac.providers.*
import no.nav.modia.soknadsstatus.ansatt.GeografiskeTilgangsRoller
import no.nav.personoversikt.common.kabac.Decision
import no.nav.personoversikt.common.kabac.Kabac
import no.nav.personoversikt.common.kabac.Kabac.EvaluationContext
import no.nav.personoversikt.common.kabac.utils.Key

class GeografiskTilgangPolicy(private val geografiskeTilgangsRoller: GeografiskeTilgangsRoller) : Kabac.Policy {
    override val key = Companion.key

    companion object {
        val key = Key<Kabac.Policy>(GeografiskTilgangPolicy::class.java)
    }

    override fun evaluate(ctx: EvaluationContext): Decision {
        val veiledersRoller = ctx.getValue(VeiledersRollerPip.key)

        if (geografiskeTilgangsRoller.nasjonaleTilgangsRoller.hasIntersection(veiledersRoller)) {
            return Decision.Permit()
        }

        val brukersEnhet: EnhetId = ctx.getValue(BrukersEnhetPip) ?: return Decision.Permit()

        val veiledersEnheter: List<EnhetId> = ctx.getValue(VeiledersEnheterPip)
        if (veiledersEnheter.contains(brukersEnhet)) {
            return Decision.Permit()
        }

        if (geografiskeTilgangsRoller.regionaleTilgangsRoller.hasIntersection(veiledersRoller)) {
            val brukersRegion: EnhetId? = ctx.getValue(BrukersRegionEnhetPip)
            val veiledersRegioner: List<EnhetId> = ctx.getValue(VeiledersRegionEnheterPip)

            if (veiledersRegioner.contains(brukersRegion)) {
                return Decision.Permit()
            }
        }

        return Decision.Deny(
            "Veileder har ikke tilgang til bruker basert på geografisk tilgang",
            DenyCauseCode.FP4_GEOGRAFISK
        )
    }
}
