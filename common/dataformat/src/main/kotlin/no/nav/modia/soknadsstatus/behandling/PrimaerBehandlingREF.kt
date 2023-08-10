package no.nav.modia.soknadsstatus.behandling

import kotlinx.serialization.Serializable

@Serializable
data class PrimaerBehandlingREF(
    val behandlingsREF: String?,
    val type: Type,
)
