package no.nav.modia.soknadsstatus.pdl

import no.nav.api.generated.pdl.enums.IdentGruppe
import no.nav.api.generated.pdl.hentadressebeskyttelse.Adressebeskyttelse
import no.nav.modia.soknadsstatus.SuspendCache
import no.nav.modia.soknadsstatus.SuspendCacheImpl
import no.nav.personoversikt.common.logging.TjenestekallLogg
import kotlin.time.Duration.Companion.minutes

class PdlOppslagServiceImpl(
    private val pdlClient: PdlClientImpl,
    private val fnrCache: SuspendCache<String, String?> = getCache(),
    private val aktorIdCache: SuspendCache<String, String?> = getCache(),
    private val geografiskTilknytningCache: SuspendCache<String, String?> = getCache(),
    private val adresseBeskyttelseCache: SuspendCache<String, List<Adressebeskyttelse>> = getCache(),
    private val identerCache: SuspendCache<String, List<String>> = getCache(),
) : PdlOppslagService {
    private val nonExistingSet = mutableSetOf<String>()

    companion object {
        fun <VALUE_TYPE> getCache(): SuspendCache<String, VALUE_TYPE> = SuspendCacheImpl(expiresAfterWrite = 1.minutes)
    }

    override suspend fun hentFnr(
        userToken: String,
        aktorId: String,
    ): String? =
        fnrCache.get(aktorId) {
            pdlClient.hentAktivIdent(
                userToken,
                aktorId,
                IdentGruppe.FOLKEREGISTERIDENT,
            )
        }

    override suspend fun hentFnrMedSystemToken(aktorId: String): String? =
        fnrCache.get(aktorId) {
            pdlClient.hentAktivIdentMedSystemToken(
                aktorId,
                IdentGruppe.FOLKEREGISTERIDENT,
            )
        }

    override suspend fun hentFnrMedSystemTokenBolk(aktorIds: List<String>): List<Pair<String, String>> {
        try {
            return pdlClient.hentAktivIdentMedSystemTokenBolk(
                aktorIds,
                listOf(IdentGruppe.FOLKEREGISTERIDENT, IdentGruppe.AKTORID, IdentGruppe.NPID),
            )
        } catch (e: IllegalArgumentException) {
            TjenestekallLogg.warn(
                "HentFnrBolk feilet",
                fields = mapOf("aktoer" to aktorIds),
            )
            return emptyList()
        }
    }

    override suspend fun hentAktorId(
        userToken: String,
        fnr: String,
    ): String? = aktorIdCache.get(fnr) { pdlClient.hentAktivIdent(userToken, fnr, IdentGruppe.AKTORID) }

    override suspend fun hentGeografiskTilknytning(
        userToken: String,
        fnr: String,
    ): String? = geografiskTilknytningCache.get(fnr) { pdlClient.hentGeografiskTilknytning(userToken, fnr) }

    override suspend fun hentAdresseBeskyttelse(
        userToken: String,
        fnr: String,
    ): List<Adressebeskyttelse> = adresseBeskyttelseCache.get(fnr) { pdlClient.hentAdresseBeskyttelse(userToken, fnr) }

    override suspend fun hentAktiveIdenter(
        userToken: String,
        fnr: String,
    ): List<String> = identerCache.get(fnr) { pdlClient.hentAktiveIdenter(userToken, fnr) }
}
