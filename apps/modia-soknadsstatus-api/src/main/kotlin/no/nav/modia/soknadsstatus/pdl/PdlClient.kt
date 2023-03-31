package no.nav.modia.soknadsstatus.pdl

import com.expediagroup.graphql.client.serializer.GraphQLClientSerializer
import com.expediagroup.graphql.client.serializer.defaultGraphQLSerializer
import com.expediagroup.graphql.client.types.GraphQLClientError
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.ktor.client.*
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.*
import io.ktor.utils.io.core.*
import no.nav.modia.soknadsstatus.utils.BoundedOnBehalfOfTokenClient
import no.nav.modia.soknadsstatus.utils.LoggingGraphQLKtorClient
import no.nav.utils.getCallId
import java.net.URL

open class PdlClient(
    private val url: URL,
    private val httpClient: HttpClient = HttpClient(engineFactory = CIO),
    private val oboTokenProvider: BoundedOnBehalfOfTokenClient,
    private val serializer: GraphQLClientSerializer = defaultGraphQLSerializer()
) : LoggingGraphQLKtorClient(
    name = "PDL",
    critical = false,
    url = url,
    httpClient = httpClient
),
    Closeable {

    private fun requestConfig(token: String): HttpRequestBuilder.() -> Unit = {
        val oboToken = oboTokenProvider.exchangeOnBehalfOfToken(token)
        header("Authorization", "Bearer $oboToken")
        header("Tema", "GEN")
        header("X-Correlation-ID", getCallId())
    }

    override suspend fun <T : Any> execute(
        request: GraphQLClientRequest<T>,
        requestCustomizer: HttpRequestBuilder.() -> Unit
    ): GraphQLClientResponse<T> {
        return try {
            super.execute(request, requestCustomizer)
        } catch (e: Exception) {
            val error = GraphQLClientException("Feilet ved oppslag mot PDL (ID: ${getCallId()})")
            GraphQLErrorResponse(errors = listOf(error))
        }
    }

    suspend fun <T : Any> executeWithToken(request: GraphQLClientRequest<T>, token: String): GraphQLClientResponse<T> {
        val requestConfig = requestConfig(token)
        return execute(request, requestConfig)
    }
    override fun close() {
        httpClient.close()
    }
}

data class GraphQLClientException(override val message: String) : GraphQLClientError

data class GraphQLErrorResponse<T>(override val errors: List<GraphQLClientError>?) : GraphQLClientResponse<T>
