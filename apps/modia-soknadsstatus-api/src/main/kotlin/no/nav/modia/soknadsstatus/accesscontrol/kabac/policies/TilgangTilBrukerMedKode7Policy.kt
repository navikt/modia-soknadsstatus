package no.nav.modia.soknadsstatus.accesscontrol.kabac.policies

import no.nav.modia.soknadsstatus.accesscontrol.DenyCauseCode
import no.nav.modia.soknadsstatus.accesscontrol.kabac.providers.BrukersDiskresjonskodePip
import no.nav.modia.soknadsstatus.accesscontrol.kabac.providers.VeiledersRollerPip
import no.nav.modia.soknadsstatus.ansatt.AnsattRolle
import no.nav.personoversikt.common.kabac.Decision
import no.nav.personoversikt.common.kabac.Kabac
import no.nav.personoversikt.common.kabac.Kabac.EvaluationContext
import no.nav.personoversikt.common.kabac.utils.Key

class TilgangTilBrukerMedKode7Policy(private val kode7Rolle: AnsattRolle) : Kabac.Policy {
    override val key = Companion.key

    companion object {
        val key = Key<Kabac.Policy>(TilgangTilBrukerMedKode7Policy::class.java.simpleName)
    }

    override fun evaluate(ctx: EvaluationContext): Decision {
        val veiledersRoller = ctx.getValue(VeiledersRollerPip.key)

        val veiledersTilgangTilKode7 = veiledersRoller.contains(kode7Rolle)

        if (veiledersTilgangTilKode7) {
            return Decision.Permit()
        }
        val diskresjonskode = ctx.getValue(BrukersDiskresjonskodePip)

        return if (diskresjonskode == BrukersDiskresjonskodePip.Kode.KODE7) {
            Decision.Deny("Veileder har ikke tilgang til kode7", DenyCauseCode.FP2_KODE7)
        } else {
            Decision.NotApplicable()
        }
    }
}
