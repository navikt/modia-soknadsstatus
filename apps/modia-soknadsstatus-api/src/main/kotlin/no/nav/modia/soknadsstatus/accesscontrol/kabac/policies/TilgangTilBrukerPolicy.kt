package no.nav.modia.soknadsstatus.accesscontrol.kabac.policies

import no.nav.modiapersonoversikt.infrastructure.tilgangskontroll.kabac.policies.TilgangTilBrukerMedKode6Policy
import no.nav.modiapersonoversikt.infrastructure.tilgangskontroll.kabac.policies.TilgangTilBrukerMedKode7Policy
import no.nav.modiapersonoversikt.infrastructure.tilgangskontroll.kabac.policies.TilgangTilBrukerMedSkjermingPolicy
import no.nav.personoversikt.common.kabac.CombiningAlgorithm
import no.nav.personoversikt.common.kabac.Kabac

object TilgangTilBrukerPolicy : Kabac.Policy by CombiningAlgorithm.denyOverride.combine(
    listOf(
        GeografiskTilgangPolicy,
        TilgangTilBrukerMedSkjermingPolicy,
        TilgangTilBrukerMedKode6Policy,
        TilgangTilBrukerMedKode7Policy
    )
)
