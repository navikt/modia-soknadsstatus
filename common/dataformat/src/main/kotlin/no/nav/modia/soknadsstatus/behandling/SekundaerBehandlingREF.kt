package no.nav.modia.soknadsstatus.behandling

import kotlinx.serialization.Serializable

@Serializable
data class SekundaerBehandlingREF(
    val behandlingsREF: String,
    val type: Type,
)
