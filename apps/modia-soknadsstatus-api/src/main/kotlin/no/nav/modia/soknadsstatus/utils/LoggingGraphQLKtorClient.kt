package no.nav.modia.soknadsstatus.utils

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.expediagroup.graphql.client.serializer.defaultGraphQLSerializer
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import io.ktor.client.*
import io.ktor.client.request.*
import no.nav.common.utils.IdUtils
import no.nav.personoversikt.common.logging.TjenestekallLogg
import no.nav.personoversikt.common.utils.SelftestGenerator
import no.nav.utils.getCallId
import java.net.URL

open class LoggingGraphQLKtorClient(
    private val name: String,
    critical: Boolean,
    url: URL,
    httpClient: HttpClient,
) : GraphQLKtorClient(url, httpClient) {
    private val selftestReporter =
        SelftestGenerator
            .Reporter(name, critical)
            .also { it.reportOk() }

    override suspend fun <T : Any> execute(
        request: GraphQLClientRequest<T>,
        requestCustomizer: HttpRequestBuilder.() -> Unit,
    ): GraphQLClientResponse<T> {
        val callId: String = getCallId()
        val requestId = IdUtils.generateId()
        try {
            TjenestekallLogg.info(
                "$name-request: $callId ($requestId)",
                mapOf(
                    "request" to defaultGraphQLSerializer().serialize(request),
                ),
            )
            val response = super.execute(request, requestCustomizer)
            val logMessage =
                mapOf(
                    "data" to response.data,
                    "errors" to response.errors,
                    "extensions" to response.extensions,
                )

            if (response.errors?.isNotEmpty() == true) {
                TjenestekallLogg.error(
                    "$name-response-error: $callId ($requestId)",
                    logMessage,
                )
                val exception = IllegalArgumentException(response.errors!!.joinToString(", ") { it.message })
                selftestReporter.reportError(exception)
                throw exception
            }
            TjenestekallLogg.info(
                "$name-response: $callId ($requestId)",
                logMessage,
            )
            selftestReporter.reportOk()
            return response
        } catch (exception: Throwable) {
            TjenestekallLogg.error(
                "$name-response-error: $callId ($requestId)",
                mapOf("exception" to exception),
            )
            selftestReporter.reportError(exception)
            throw exception
        }
    }
}
