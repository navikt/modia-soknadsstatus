package no.nav.modia.soknadsstatus.pdl

interface PdlOppslagService {
    fun hentFnr(aktorId: String, token: String): String?
}
