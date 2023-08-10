package no.nav.modia.soknadsstatus.behandling

import kotlinx.serialization.Serializable

@Serializable
data class Avslutningsstatus(
    val kodeRef: String?,
    val kodeverksRef: String?,
    val value: String,
)
