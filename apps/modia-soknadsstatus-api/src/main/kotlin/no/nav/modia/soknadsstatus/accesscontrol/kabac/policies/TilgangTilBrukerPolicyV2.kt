package no.nav.modia.soknadsstatus.accesscontrol.kabac.policies

import no.nav.personoversikt.common.kabac.CombiningAlgorithm
import no.nav.personoversikt.common.kabac.Kabac

class TilgangTilBrukerPolicyV2 :
    Kabac.Policy by CombiningAlgorithm.denyOverride.combine(
        listOf(
            TilgangsMaskinenPolicy,
        ),
    )
