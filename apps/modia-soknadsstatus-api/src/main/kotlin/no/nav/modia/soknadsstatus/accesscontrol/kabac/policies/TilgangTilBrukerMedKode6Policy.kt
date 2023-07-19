package no.nav.modia.soknadsstatus.accesscontrol.kabac.policies

import no.nav.modia.soknadsstatus.accesscontrol.DenyCauseCode
import no.nav.modia.soknadsstatus.accesscontrol.kabac.providers.BrukersDiskresjonskodePip
import no.nav.modia.soknadsstatus.accesscontrol.kabac.providers.VeiledersRollerPip
import no.nav.modia.soknadsstatus.ansatt.AnsattRolle
import no.nav.personoversikt.common.kabac.Decision
import no.nav.personoversikt.common.kabac.Kabac
import no.nav.personoversikt.common.kabac.Kabac.EvaluationContext
import no.nav.personoversikt.common.kabac.utils.Key

class TilgangTilBrukerMedKode6Policy(private val kode6Rolle: AnsattRolle) : Kabac.Policy {
    override val key = Companion.key

    companion object {
        val key = Key<Kabac.Policy>(TilgangTilBrukerMedKode6Policy::class.java)
    }

    override fun toString(): String {
        return "TilgangTilBrukerMedKode6Policy"
    }

    override fun evaluate(ctx: EvaluationContext): Decision {
        val veiledersRoller = ctx.getValue(VeiledersRollerPip.key)
        val veiledersTilgangTilKode6 = veiledersRoller.contains(kode6Rolle)

        if (veiledersTilgangTilKode6) {
            return Decision.Permit()
        }
        val diskresjonskode = ctx.getValue(BrukersDiskresjonskodePip)

        return if (diskresjonskode == BrukersDiskresjonskodePip.Kode.KODE6) {
            Decision.Deny("Veileder har ikke tilgang til kode6", DenyCauseCode.FP1_KODE6)
        } else {
            Decision.NotApplicable()
        }
    }
}
