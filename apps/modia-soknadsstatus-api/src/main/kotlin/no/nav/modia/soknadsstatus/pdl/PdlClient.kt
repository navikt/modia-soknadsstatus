package no.nav.modia.soknadsstatus.pdl

import com.expediagroup.graphql.client.types.GraphQLClientError
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.ktor.client.*
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.runBlocking
import no.nav.api.generated.pdl.HentAdressebeskyttelse
import no.nav.api.generated.pdl.HentAktorid
import no.nav.api.generated.pdl.HentGeografiskTilknyttning
import no.nav.api.generated.pdl.enums.IdentGruppe
import no.nav.api.generated.pdl.hentadressebeskyttelse.Adressebeskyttelse
import no.nav.modia.soknadsstatus.accesscontrol.RestConstants
import no.nav.modia.soknadsstatus.utils.BoundedMachineToMachineTokenClient
import no.nav.modia.soknadsstatus.utils.BoundedOnBehalfOfTokenClient
import no.nav.modia.soknadsstatus.utils.LoggingGraphQLKtorClient
import no.nav.utils.getCallId
import java.net.URL

typealias HeadersBuilder = HttpRequestBuilder.() -> Unit

open class PdlClient(
    private val oboTokenProvider: BoundedOnBehalfOfTokenClient,
    private val machineToMachineTokenClient: BoundedMachineToMachineTokenClient,
    url: URL,
    private val httpClient: HttpClient = HttpClient(engineFactory = CIO),
) : LoggingGraphQLKtorClient(
    name = "PDL",
    critical = false,
    url = url,
    httpClient = httpClient,
),
    Closeable {

    suspend fun hentGeografiskTilknytning(fnr: String, userToken: String): String? =
        execute(
            HentGeografiskTilknyttning(HentGeografiskTilknyttning.Variables(fnr)),
            userTokenAuthorizationHeaders(userToken),
        )
            .data
            ?.hentGeografiskTilknytning
            ?.run {
                gtBydel ?: gtKommune ?: gtLand
            }

    suspend fun hentAdresseBeskyttelse(fnr: String, userToken: String): List<Adressebeskyttelse> =
        execute(
            HentAdressebeskyttelse(HentAdressebeskyttelse.Variables(fnr)),
            userTokenAuthorizationHeaders(userToken),
        )
            .data?.hentPerson?.adressebeskyttelse
            ?: emptyList()


    suspend fun hentAktivIdent(ident: String, gruppe: IdentGruppe, userToken: String): String? =
        execute(HentAktorid(HentAktorid.Variables(ident, listOf(gruppe))), userTokenAuthorizationHeaders(userToken))
            .data
            ?.hentIdenter
            ?.identer
            ?.firstOrNull()
            ?.ident


    suspend fun hentAktivIdent(ident: String, gruppe: IdentGruppe): String? =
        execute(HentAktorid(HentAktorid.Variables(ident, listOf(gruppe))), systemTokenAuthorizationHeaders)
            .data
            ?.hentIdenter
            ?.identer
            ?.firstOrNull()
            ?.ident


    override suspend fun <T : Any> execute(
        request: GraphQLClientRequest<T>,
        requestCustomizer: HttpRequestBuilder.() -> Unit,
    ): GraphQLClientResponse<T> {
        return try {
            super.execute(request, requestCustomizer)
        } catch (e: Exception) {
            val error = GraphQLClientException("Feilet ved oppslag mot PDL (ID: ${getCallId()})")
            GraphQLErrorResponse(errors = listOf(error))
        }
    }

    override fun close() {
        httpClient.close()
    }

    private fun userTokenAuthorizationHeaders(userToken: String): HeadersBuilder = {
        val exchangedToken = oboTokenProvider.exchangeOnBehalfOfToken(userToken)
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

data class GraphQLClientException(override val message: String) : GraphQLClientError

data class GraphQLErrorResponse<T>(override val errors: List<GraphQLClientError>?) : GraphQLClientResponse<T>
