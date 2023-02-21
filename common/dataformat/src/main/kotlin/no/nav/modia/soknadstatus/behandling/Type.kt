package no.nav.modia.soknadstatus.behandling

import kotlinx.serialization.Serializable

@Serializable
data class Type(
    val kodeRef: String,
    val kodeverksRef: String,
    val value: String
)