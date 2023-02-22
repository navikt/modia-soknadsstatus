package no.nav.modia.soknadstatus.pdl

interface PdlOppslagService {
    fun hentFnr(aktorId: String): String?
}
