package no.nav.modia.soknadsstatus.pdl

import no.nav.api.generated.pdl.hentadressebeskyttelse.Adressebeskyttelse
import no.nav.modia.soknadsstatus.MockData

class PdlOppslagServiceMock : PdlOppslagService {
    override fun hentFnr(userToken: String, aktorId: String): String = MockData.bruker.fnr
    override fun hentFnrMedSystemToken(aktorId: String): String = MockData.bruker.fnr
    override fun hentAktorId(userToken: String, fnr: String): String = MockData.bruker.aktorId

    override fun hentGeografiskTilknytning(userToken: String, fnr: String): String = MockData.bruker.geografiskTilknyttning

    override fun hentAdresseBeskyttelse(userToken: String, fnr: String): List<Adressebeskyttelse> = mutableListOf()
    override fun hentIdenter(userToken: String, fnr: String): List<String> = listOf(MockData.bruker.fnr)
}
