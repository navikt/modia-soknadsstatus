package no.nav.modia.soknadsstatus.pdl

import io.ktor.server.auth.*
import no.nav.api.generated.pdl.hentadressebeskyttelse.Adressebeskyttelse

interface PdlOppslagService {
    fun hentFnr(userToken: String, aktorId: String): String?
    fun hentFnrMedSystemToken(aktorId: String): String?
    fun hentAktorId(userToken: String, fnr: String): String?
    fun hentGeografiskTilknytning(userToken: String, fnr: String): String?
    fun hentAdresseBeskyttelse(userToken: String, fnr: String): List<Adressebeskyttelse>
    fun hentIdenter(userToken: String, fnr: String): List<String>
}
