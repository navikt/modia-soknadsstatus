package no.nav.modia.soknadsstatus.pdl

import com.github.benmanes.caffeine.cache.Cache
import no.nav.api.generated.pdl.enums.IdentGruppe
import no.nav.api.generated.pdl.hentadressebeskyttelse.Adressebeskyttelse

class PdlOppslagServiceImpl(
    private val pdlClient: PdlClient,
    private val fnrCache: Cache<String, String>,
    private val aktorIdCache: Cache<String, String>,
    private val geografiskTilknytningCache: Cache<String, String>,
    private val adresseBeskyttelseCache: Cache<String, List<Adressebeskyttelse>>,
) : PdlOppslagService {

    override suspend fun hentFnr(userToken: String, aktorId: String): String? =
        fnrCache.getIfPresent(aktorId) ?: pdlClient.hentAktivIdent(
            aktorId,
            IdentGruppe.FOLKEREGISTERIDENT,
            userToken,
        )


    override suspend fun hentFnrMedSystemToken(aktorId: String): String? =
        fnrCache.getIfPresent(aktorId) ?: pdlClient.hentAktivIdent(
            aktorId,
            IdentGruppe.FOLKEREGISTERIDENT,
        )

    override suspend fun hentAktorId(userToken: String, fnr: String): String? =
        aktorIdCache.getIfPresent(fnr) ?: pdlClient.hentAktivIdent(fnr, IdentGruppe.AKTORID, userToken)

    override suspend fun hentGeografiskTilknytning(userToken: String, fnr: String): String? =
        geografiskTilknytningCache.getIfPresent(fnr) ?: pdlClient.hentGeografiskTilknytning(fnr, userToken)

    override suspend fun hentAdresseBeskyttelse(userToken: String, fnr: String): List<Adressebeskyttelse> =
        adresseBeskyttelseCache.getIfPresent(fnr) ?: pdlClient.hentAdresseBeskyttelse(fnr, userToken)
}
