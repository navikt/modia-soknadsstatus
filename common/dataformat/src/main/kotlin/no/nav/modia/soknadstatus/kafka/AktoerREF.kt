package no.nav.modia.soknadstatus.kafka

import kotlinx.serialization.Serializable

@Serializable
data class AktoerREF(
    val aktoerId: String
)
