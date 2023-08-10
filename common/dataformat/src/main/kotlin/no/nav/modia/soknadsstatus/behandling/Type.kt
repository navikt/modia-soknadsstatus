package no.nav.modia.soknadsstatus.behandling

import kotlinx.serialization.Serializable

@Serializable
data class Type(
    val kodeRef: String? = null,
    val kodeverksRef: String? = null,
    val value: String,
)
