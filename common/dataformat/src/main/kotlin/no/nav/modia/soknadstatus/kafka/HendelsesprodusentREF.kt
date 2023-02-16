package no.nav.modia.soknadstatus.kafka

import kotlinx.serialization.Serializable

@Serializable
data class HendelsesprodusentREF(
    val kodeRef: String,
    val kodeverksRef: String,
    val value: String
)
