package no.nav.modia.soknadstatus.kafka

import kotlinx.serialization.Serializable

@Serializable
data class PrimaerBehandlingREF(
    val behandlingsREF: String,
    val type: Type
)
