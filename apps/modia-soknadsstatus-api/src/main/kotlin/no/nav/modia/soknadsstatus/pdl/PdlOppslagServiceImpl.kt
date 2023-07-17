package no.nav.modia.soknadsstatus.pdl

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.api.generated.pdl.enums.IdentGruppe
import no.nav.api.generated.pdl.hentadressebeskyttelse.Adressebeskyttelse
import java.util.concurrent.TimeUnit

class PdlOppslagServiceImpl(
    private val pdlClient: PdlClientImpl,
    private val fnrCache: Cache<String, String> = getCache(),
    private val aktorIdCache: Cache<String, String> = getCache(),
    private val geografiskTilknytningCache: Cache<String, String> = getCache(),
    private val adresseBeskyttelseCache: Cache<String, List<Adressebeskyttelse>> = getCache(),
    private val identerCache: Cache<String, List<String>> = getCache()
) : PdlOppslagService {
    companion object {
        fun <VALUE_TYPE> getCache(): Cache<String, VALUE_TYPE> =
            Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build()
    }

    override suspend fun hentFnr(userToken: String, aktorId: String): String? =
        fnrCache.getIfPresent(aktorId) ?: pdlClient.hentAktivIdent(
            userToken,
            aktorId,
            IdentGruppe.FOLKEREGISTERIDENT,
        )

    override suspend fun hentFnrMedSystemToken(aktorId: String): String? =
        fnrCache.getIfPresent(aktorId) ?: pdlClient.hentAktivIdentMedSystemToken(
            aktorId,
            IdentGruppe.FOLKEREGISTERIDENT,
        )

    override suspend fun hentAktorId(userToken: String, fnr: String): String? =
        aktorIdCache.getIfPresent(fnr) ?: pdlClient.hentAktivIdent(userToken, fnr, IdentGruppe.AKTORID)

    override suspend fun hentGeografiskTilknytning(userToken: String, fnr: String): String? =
        geografiskTilknytningCache.getIfPresent(fnr) ?: pdlClient.hentGeografiskTilknytning(userToken, fnr)

    override suspend fun hentAdresseBeskyttelse(userToken: String, fnr: String): List<Adressebeskyttelse> =
        adresseBeskyttelseCache.getIfPresent(fnr) ?: pdlClient.hentAdresseBeskyttelse(userToken, fnr)

    override suspend fun hentAktiveIdenter(userToken: String, fnr: String): List<String> =
        identerCache.getIfPresent(fnr) ?: pdlClient.hentAktiveIdenter(userToken, fnr)
}
