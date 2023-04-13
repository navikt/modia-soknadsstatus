package no.nav.modia.soknadsstatus.pdl

import com.expediagroup.graphql.client.types.GraphQLClientError
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.ktor.client.*
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.*
import io.ktor.utils.io.core.*
import no.nav.modia.soknadsstatus.utils.LoggingGraphQLKtorClient
import no.nav.utils.getCallId
import java.net.URL

typealias HeadersBuilder = HttpRequestBuilder.() -> Unit

open class PdlClient(
    url: URL,
    private val httpClient: HttpClient = HttpClient(engineFactory = CIO),
) : LoggingGraphQLKtorClient(
    name = "PDL",
    critical = false,
    url = url,
    httpClient = httpClient
),
    Closeable {

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

    override fun close() {
        httpClient.close()
    }
}

data class GraphQLClientException(override val message: String) : GraphQLClientError

data class GraphQLErrorResponse<T>(override val errors: List<GraphQLClientError>?) : GraphQLClientResponse<T>
