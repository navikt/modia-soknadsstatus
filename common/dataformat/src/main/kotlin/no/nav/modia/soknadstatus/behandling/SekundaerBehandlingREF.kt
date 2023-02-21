package no.nav.modia.soknadstatus.behandling

import kotlinx.serialization.Serializable

@Serializable
data class SekundaerBehandlingREF(
    val behandlingsREF: String,
    val type: Type
)
