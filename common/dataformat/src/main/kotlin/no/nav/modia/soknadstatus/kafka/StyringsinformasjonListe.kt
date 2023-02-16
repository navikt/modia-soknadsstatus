package no.nav.modia.soknadstatus.kafka

import kotlinx.serialization.Serializable

@Serializable
data class StyringsinformasjonListe(
    val key: String,
    val type: String,
    val value: String
)
