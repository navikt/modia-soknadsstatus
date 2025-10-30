package no.nav.modia.soknadsstatus.ansatt

class GeografiskeTilgangsRoller {
    val nasjonaleTilgangsRoller =
        RolleListe(
            "0000-ga-gosys_nasjonal",
            "0000-ga-gosys_utvidbar_til_nasjonal",
            "0000-ga-pensjon_nasjonal_u_logg",
            "0000-ga-pensjon_nasjonal_m_logg",
        )
    val regionaleTilgangsRoller =
        RolleListe(
            "0000-ga-gosys_regional",
            "0000-ga-gosys_utvidbar_til_regional",
        )
}
