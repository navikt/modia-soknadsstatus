package no.nav.modia.soknadsstatus

fun String.removeBearerFromToken(): String = this.substringAfter("Bearer ")
