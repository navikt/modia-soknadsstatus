package no.nav.modia.soknadsstatus.accesscontrol.kabac.policies

import no.nav.modia.soknadsstatus.accesscontrol.DenyCauseCode
import no.nav.modia.soknadsstatus.accesscontrol.kabac.providers.BrukersSkjermingPip
import no.nav.modia.soknadsstatus.accesscontrol.kabac.providers.VeiledersRollerPip
import no.nav.modia.soknadsstatus.ansatt.AnsattRolle
import no.nav.personoversikt.common.kabac.Decision
import no.nav.personoversikt.common.kabac.Kabac
import no.nav.personoversikt.common.kabac.Kabac.EvaluationContext
import no.nav.personoversikt.common.kabac.utils.Key

class TilgangTilBrukerMedSkjermingPolicy(private val ansattRolle: AnsattRolle) : Kabac.Policy {
    override val key = Companion.key

    companion object {
        val key = Key<Kabac.Policy>(TilgangTilBrukerMedSkjermingPolicy::class.java.simpleName)
    }

    override fun evaluate(ctx: EvaluationContext): Decision {
        val veilederRoller = ctx.getValue(VeiledersRollerPip.key)

        if (veilederRoller.contains(ansattRolle)) {
            return Decision.Permit()
        }
        val erSkjermet = ctx.getValue(BrukersSkjermingPip)

        return if (erSkjermet) {
            Decision.Deny("Veileder har ikke tilgang til skjermet person", DenyCauseCode.FP3_EGEN_ANSATT)
        } else {
            Decision.NotApplicable()
        }
    }
}
