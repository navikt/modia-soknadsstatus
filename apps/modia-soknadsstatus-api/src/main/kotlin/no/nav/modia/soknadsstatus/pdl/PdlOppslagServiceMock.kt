package no.nav.modia.soknadsstatus.pdl

import no.nav.api.generated.pdl.hentadressebeskyttelse.Adressebeskyttelse
import no.nav.modia.soknadsstatus.MockData

class PdlOppslagServiceMock : PdlOppslagService {
    override suspend fun hentFnr(
        userToken: String,
        aktorId: String,
    ): String = MockData.Bruker.fnr

    override suspend fun hentFnrMedSystemToken(aktorId: String): String = MockData.Bruker.fnr

    override suspend fun hentFnrMedSystemTokenBolk(aktorIds: List<String>): List<Pair<String, String>> =
        listOf(
            MockData.Bruker.aktorId to MockData.Bruker.fnr,
        )

    override suspend fun hentAktorId(
        userToken: String,
        fnr: String,
    ): String = MockData.Bruker.aktorId

    override suspend fun hentGeografiskTilknytning(
        userToken: String,
        fnr: String,
    ): String = MockData.Bruker.geografiskTilknyttning

    override suspend fun hentAdresseBeskyttelse(
        userToken: String,
        fnr: String,
    ): List<Adressebeskyttelse> = mutableListOf()

    override suspend fun hentAktiveIdenter(
        userToken: String,
        fnr: String,
    ): List<String> = listOf(MockData.Bruker.fnr)
}
