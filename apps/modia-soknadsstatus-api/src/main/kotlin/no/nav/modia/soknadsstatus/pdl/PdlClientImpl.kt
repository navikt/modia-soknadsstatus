package no.nav.modia.soknadsstatus.pdl

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.utils.io.core.*
import no.nav.api.generated.pdl.*
import no.nav.api.generated.pdl.enums.IdentGruppe
import no.nav.api.generated.pdl.hentadressebeskyttelse.Adressebeskyttelse
import no.nav.modia.soknadsstatus.accesscontrol.RestConstants
import no.nav.modia.soknadsstatus.removeBearerFromToken
import no.nav.modia.soknadsstatus.utils.BoundedMachineToMachineTokenClient
import no.nav.modia.soknadsstatus.utils.BoundedOnBehalfOfTokenClient
import no.nav.modia.soknadsstatus.utils.LoggingGraphQLKtorClient
import no.nav.personoversikt.common.logging.TjenestekallLogg
import no.nav.utils.getCallId
import java.net.URL

typealias HeadersBuilder = HttpRequestBuilder.() -> Unit

interface PdlClient {
    suspend fun hentGeografiskTilknytning(
        userToken: String,
        fnr: String,
    ): String?

    suspend fun hentAdresseBeskyttelse(
        userToken: String,
        fnr: String,
    ): List<Adressebeskyttelse>

    suspend fun hentAktivIdent(
        userToken: String,
        aktoerId: String,
        gruppe: IdentGruppe,
    ): String?

    suspend fun hentAktivIdentMedSystemToken(
        aktoerId: String,
        gruppe: IdentGruppe,
    ): String?

    suspend fun hentAktivIdentMedSystemTokenBolk(
        aktoerIds: List<String>,
        grupper: List<IdentGruppe>,
    ): List<Pair<String, String>>

    suspend fun hentAktiveIdenter(
        userToken: String,
        ident: String,
    ): List<String>
}

class PdlClientImpl(
    private val oboTokenProvider: BoundedOnBehalfOfTokenClient,
    private val machineToMachineTokenClient: BoundedMachineToMachineTokenClient,
    url: URL,
    private val httpClient: HttpClient = HttpClient(OkHttp.create()),
) : LoggingGraphQLKtorClient(
        name = "PDL",
        critical = false,
        url = url,
        httpClient = httpClient,
    ),
    PdlClient,
    Closeable {
    override suspend fun hentGeografiskTilknytning(
        userToken: String,
        fnr: String,
    ): String? =
        execute(
            HentGeografiskTilknyttning(HentGeografiskTilknyttning.Variables(fnr)),
            userTokenAuthorizationHeaders(userToken),
        ).data
            ?.hentGeografiskTilknytning
            ?.run {
                gtBydel ?: gtKommune ?: gtLand
            }

    override suspend fun hentAdresseBeskyttelse(
        userToken: String,
        fnr: String,
    ): List<Adressebeskyttelse> =
        execute(
            HentAdressebeskyttelse(HentAdressebeskyttelse.Variables(fnr)),
            userTokenAuthorizationHeaders(userToken),
        ).data
            ?.hentPerson
            ?.adressebeskyttelse
            ?: emptyList()

    override suspend fun hentAktivIdent(
        userToken: String,
        aktoerId: String,
        gruppe: IdentGruppe,
    ): String? =
        execute(HentIdenter(HentIdenter.Variables(aktoerId, listOf(gruppe))), userTokenAuthorizationHeaders(userToken))
            .data
            ?.hentIdenter
            ?.identer
            ?.firstOrNull()
            ?.ident

    override suspend fun hentAktivIdentMedSystemToken(
        aktoerId: String,
        gruppe: IdentGruppe,
    ): String? =
        execute(HentIdenter(HentIdenter.Variables(aktoerId, listOf(gruppe))), systemTokenAuthorizationHeaders)
            .data
            ?.hentIdenter
            ?.identer
            ?.firstOrNull()
            ?.ident

    override suspend fun hentAktivIdentMedSystemTokenBolk(
        aktoerIds: List<String>,
        grupper: List<IdentGruppe>,
    ): List<Pair<String, String>> =
        execute(
            HentIdenterBolk(HentIdenterBolk.Variables(aktoerIds, grupper)),
            systemTokenAuthorizationHeaders,
        ).data
            ?.hentIdenterBolk
            ?.mapNotNull { p ->
                val aktorId = p.identer?.first { it.gruppe == IdentGruppe.AKTORID }?.ident
                var fnr: String? = null
                try {
                    fnr = p.identer?.first { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }?.ident
                } catch (e: NoSuchElementException) {
                    TjenestekallLogg.warn("Fant ikke ident med gruppe FOLKEREGISTERIDENT, prøver NPID", mapOf("AktorID" to aktorId))
                    try {
                        fnr = p.identer?.first { it.gruppe == IdentGruppe.NPID }?.ident
                    } catch (e: NoSuchElementException) {
                        TjenestekallLogg.error("Fant ikke ident med gruppe NPID. AktorIDen ignoreres", mapOf("AktorID" to aktorId))
                    }
                }
                if (aktorId == null || fnr == null) null else aktorId to fnr
            }
            ?: emptyList()

    override suspend fun hentAktiveIdenter(
        userToken: String,
        ident: String,
    ): List<String> =
        execute(
            HentIdenter(HentIdenter.Variables(ident)),
            userTokenAuthorizationHeaders(userToken),
        ).data?.hentIdenter?.identer?.map { it.ident } ?: emptyList()

    override fun close() {
        httpClient.close()
    }

    private fun userTokenAuthorizationHeaders(userToken: String): HeadersBuilder =
        {
            val exchangedToken = oboTokenProvider.exchangeOnBehalfOfToken(userToken.removeBearerFromToken())
            header(
                RestConstants.AUTHORIZATION,
                RestConstants.AUTH_METHOD_BEARER + RestConstants.AUTH_SEPERATOR + exchangedToken,
            )
            header(RestConstants.TEMA_HEADER, RestConstants.ALLE_TEMA_HEADERVERDI)
            header("X-Correlation-ID", getCallId())
        }

    private val systemTokenAuthorizationHeaders: HeadersBuilder = {
        val systemuserToken: String = machineToMachineTokenClient.createMachineToMachineToken()
        header(
            RestConstants.AUTHORIZATION,
            RestConstants.AUTH_METHOD_BEARER + RestConstants.AUTH_SEPERATOR + systemuserToken,
        )
        header(RestConstants.TEMA_HEADER, RestConstants.ALLE_TEMA_HEADERVERDI)
        header("X-Correlation-ID", getCallId())
    }
}
