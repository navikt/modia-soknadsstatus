package no.nav.modia.soknadsstatus.pdl

import com.expediagroup.graphql.client.GraphQLClient
import com.expediagroup.graphql.client.serializer.GraphQLClientSerializer
import com.expediagroup.graphql.client.serializer.defaultGraphQLSerializer
import com.expediagroup.graphql.client.types.GraphQLClientError
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.core.*
import no.nav.personoversikt.common.logging.Logging.secureLog
import java.net.URL

open class PdlClient(
    private val url: URL,
    private val httpClient: HttpClient = HttpClient(engineFactory = CIO),
    private val serializer: GraphQLClientSerializer = defaultGraphQLSerializer()
) : GraphQLClient<HttpRequestBuilder>, Closeable {
    override suspend fun <T : Any> execute(
        request: GraphQLClientRequest<T>,
        requestCustomizer: HttpRequestBuilder.() -> Unit
    ): GraphQLClientResponse<T> {
        val callId = "TODO"
        return try {
            val rawResult: String = httpClient.post(url) {
                expectSuccess = true
                apply(requestCustomizer)
                headers["Nav-Call-Id"] = callId
                setBody(TextContent(serializer.serialize(request), ContentType.Application.Json))
            }.body()
            serializer.deserialize(rawResult, request.responseType())
        } catch (e: Exception) {
            secureLog.error("Feilet ved oppslag mot PDL (ID: $callId)", e)
            val error = GraphQLClientException("Feilet ved oppslag mot PDL (ID: $callId)")
            GraphQLErrorResponse(errors = listOf(error))
        }
    }

    override fun close() {
        httpClient.close()
    }

    override suspend fun execute(
        requests: List<GraphQLClientRequest<*>>,
        requestCustomizer: HttpRequestBuilder.() -> Unit
    ): List<GraphQLClientResponse<*>> {
        TODO("Not yet implemented")
    }

//    private fun getCallId(): String = MDC.get(MDCConstants.MDC_CALL_ID) ?: UUID.randomUUID().toString()
}

data class GraphQLClientException(override val message: String) : GraphQLClientError

data class GraphQLErrorResponse<T>(override val errors: List<GraphQLClientError>?) : GraphQLClientResponse<T>
