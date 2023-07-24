package no.nav.modia.soknadsstatus

fun String.removeBearerFromToken(): String {
    return this.substringAfter("Bearer ")
}
