package no.nav.modia.soknadsstatus.pdl

class PdlOppslagServiceTestImpl : PdlOppslagService {
    override fun hentFnr(aktorId: String, token: String): String? {
        val chance = Math.random()
        return if (chance > 0.2) {
            return aktorId
        } else {
            null
        }
    }
}
