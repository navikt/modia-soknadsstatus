package no.nav.modia.soknadsstatus.pdl

class PdlOppslagServiceTestImpl : PdlOppslagService {
    override fun hentFnr(aktorId: String): String? {
        val chance = Math.random() + 0.3
        return if (chance > 0.5) {
            return aktorId
        } else null
    }
}
