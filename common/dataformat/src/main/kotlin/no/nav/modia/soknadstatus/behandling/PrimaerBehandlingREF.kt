package no.nav.modia.soknadstatus.behandling

import kotlinx.serialization.Serializable

@Serializable
data class PrimaerBehandlingREF(
    val behandlingsREF: String,
    val type: Type
)
