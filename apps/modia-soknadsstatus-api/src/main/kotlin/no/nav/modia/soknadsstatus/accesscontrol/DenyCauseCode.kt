package no.nav.modia.soknadsstatus.accesscontrol

import no.nav.personoversikt.common.kabac.Decision

enum class DenyCauseCode : Decision.DenyCause {
    TILGANGSMASKINEN,
    TILGANGSMASKINEN_OVERRIDABLE,
}
