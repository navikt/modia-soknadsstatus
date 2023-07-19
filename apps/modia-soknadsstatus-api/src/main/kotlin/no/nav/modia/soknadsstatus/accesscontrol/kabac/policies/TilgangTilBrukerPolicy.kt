package no.nav.modia.soknadsstatus.accesscontrol.kabac.policies

import no.nav.modia.soknadsstatus.ansatt.GeografiskeTilgangsRoller
import no.nav.modia.soknadsstatus.ansatt.SensitiveTilgangsRoller
import no.nav.personoversikt.common.kabac.CombiningAlgorithm
import no.nav.personoversikt.common.kabac.Kabac

class TilgangTilBrukerPolicy(
    sensitiveTilgangsRoller: SensitiveTilgangsRoller,
    geografiskeTilgangsRoller: GeografiskeTilgangsRoller,
) : Kabac.Policy by CombiningAlgorithm.denyOverride.combine(
    listOf(
        GeografiskTilgangPolicy(geografiskeTilgangsRoller),
        TilgangTilBrukerMedSkjermingPolicy(sensitiveTilgangsRoller.skjermedePersoner),
        TilgangTilBrukerMedKode6Policy(sensitiveTilgangsRoller.kode6),
        TilgangTilBrukerMedKode7Policy(sensitiveTilgangsRoller.kode7)
    )
)
