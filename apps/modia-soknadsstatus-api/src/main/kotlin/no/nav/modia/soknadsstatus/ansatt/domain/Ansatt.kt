package no.nav.modia.soknadsstatus.ansatt.domain

import kotlinx.serialization.Serializable

@Serializable
data class Ansatt(
    val fornavn: String,
    val etternavn: String,
    val ident: String,
)
