package no.nav.modia.soknadsstatus.pdl

import no.nav.api.generated.pdl.hentadressebeskyttelse.Adressebeskyttelse

interface PdlOppslagService {
    suspend fun hentFnr(
        userToken: String,
        aktorId: String,
    ): String?

    suspend fun hentFnrMedSystemToken(aktorId: String): String?

    suspend fun hentFnrMedSystemTokenBolk(aktorIds: List<String>): List<Pair<String, String>>

    suspend fun hentAktorId(
        userToken: String,
        fnr: String,
    ): String?

    suspend fun hentGeografiskTilknytning(
        userToken: String,
        fnr: String,
    ): String?

    suspend fun hentAdresseBeskyttelse(
        userToken: String,
        fnr: String,
    ): List<Adressebeskyttelse>

    suspend fun hentAktiveIdenter(
        userToken: String,
        fnr: String,
    ): List<String>
}
