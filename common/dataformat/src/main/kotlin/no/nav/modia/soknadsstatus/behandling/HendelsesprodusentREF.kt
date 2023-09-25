package no.nav.modia.soknadsstatus.behandling

import kotlinx.serialization.Serializable

@Serializable
data class HendelsesprodusentREF(
    val kodeverksRef: String? = null,
    val kodeRef: String? = null,
    val value: String,
)
