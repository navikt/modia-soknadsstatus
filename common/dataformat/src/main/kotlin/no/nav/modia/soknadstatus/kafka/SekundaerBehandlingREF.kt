package no.nav.modia.soknadstatus.kafka

import kotlinx.serialization.Serializable

@Serializable
data class SekundaerBehandlingREF(
    val behandlingsREF: String,
    val type: Type
)
