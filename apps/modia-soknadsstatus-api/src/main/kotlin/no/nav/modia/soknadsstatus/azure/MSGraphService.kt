package no.nav.modia.soknadsstatus.azure

import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import no.nav.common.types.identer.AzureObjectId
import no.nav.common.types.identer.NavIdent
import no.nav.modia.soknadsstatus.ansatt.AnsattRolle
import no.nav.modia.soknadsstatus.ansatt.RolleListe
import no.nav.modia.soknadsstatus.utils.BoundedOnBehalfOfTokenClient
import no.nav.personoversikt.common.logging.TjenestekallLogg
import okhttp3.OkHttpClient
import okhttp3.Request
import java.lang.IllegalArgumentException

interface MSGraphService {
    suspend fun fetchMultipleGroupsIfUserIsMember(
        userToken: String,
        veilederIdent: NavIdent,
        groups: RolleListe,
    ): RolleListe
}

class AzureADServiceImpl(
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val tokenClient: BoundedOnBehalfOfTokenClient,
    private val graphUrl: Url,
) : MSGraphService {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchMultipleGroupsIfUserIsMember(
        userToken: String,
        veilederIdent: NavIdent,
        groups: RolleListe,
    ): RolleListe {
        val url = URLBuilder(graphUrl).apply {
            path("v1.0/me/memberOf/microsoft.graph.group")
            parameters.append("\$filter", "id in (${groups.joinToString(separator = ", ") { "'${it.gruppeId.get()}'" }})")
            parameters.append("\$count", "true")
        }.buildString()

        try {
            val response = handleRequest(url, userToken, veilederIdent)
            return RolleListe(
                response.value.map {
                    AnsattRolle(
                        gruppeNavn = requireNotNull(it.displayName),
                        gruppeId = AzureObjectId(requireNotNull(it.id)),
                    )
                },
            )
        } catch (e: Exception) {
            TjenestekallLogg.error("Kall til azureAD feilet", throwable = e, fields = mapOf())
            return RolleListe()
        }
    }

    private fun handleRequest(
        url: String,
        userToken: String,
        veilederIdent: NavIdent,
    ): AzureCountResponse<List<AzureGroupResponse>> {
        val token = tokenClient.exchangeOnBehalfOfToken(userToken)

        val request = Request.Builder().url(url).addHeader("Authorization", "Bearer $token")
            .addHeader("ConsistencyLevel", "eventual").build()
        val response = httpClient.newCall(request).execute()

        val body = response.body
            ?: throw IllegalArgumentException("Mottok ingen grupper fra MS Graph for veileder:  $veilederIdent. Body var null")

        if (!response.isSuccessful) {
            throw IllegalArgumentException(
                "Mottok ingen grupper fra MS Graph for veileder:  $veilederIdent. Body var ${
                    json.decodeFromString(
                        AzureErrorResponse.serializer(),
                        body.string(),
                    )
                }",
            )
        }

        return json.decodeFromString(
            AzureCountResponse.serializer(
                ListSerializer(AzureGroupResponse.serializer()),
            ),
            body.string(),
        )
    }
}

data class MsGraphEnv(
    val url: String,
    val scope: String,
)

@Serializable
private data class AzureCountResponse<BODY>(
    @SerialName("@odata.count")
    val count: Int,
    val value: BODY,
)

@Serializable
private data class AzureErrorResponse(
    val error: NestedAzureErrorResponse,
)

@Serializable
private data class NestedAzureErrorResponse(
    val code: String,
    val message: String,
)

@Serializable
private data class AzureGroupResponse(
    val id: String? = null,
    val deletedDateTime: String? = null,
    val classification: String? = null,
    val createdDateTime: String? = null,
    val creationOptions: List<String> = listOf(),
    val description: String? = null,
    val displayName: String? = null,
    val expirationDateTime: String? = null,
    val groupTypes: List<String> = listOf(),
    val isAssignableToRole: String? = null,
    val mail: String? = null,
    val mailEnabled: Boolean? = null,
    val mailNickname: String? = null,
    val membershipRule: String? = null,
    val membershipRuleProcessingState: String? = null,
    val onPremisesDomainName: String? = null,
    val onPremisesLastSyncDateTime: String? = null,
    val onPremisesNetBiosName: String? = null,
    val onPremisesSamAccountName: String? = null,
    val onPremisesSecurityIdentifier: String? = null,
    val onPremisesSyncEnabled: Boolean? = null,
    val preferredDataLocation: String? = null,
    val preferredLanguage: String? = null,
    val proxyAddresses: List<String> = listOf(),
    val renewedDateTime: String? = null,
    val resourceBehaviorOptions: List<String> = listOf(),
    val resourceProvisioningOptions: List<String> = listOf(),
    val securityEnabled: Boolean? = null,
    val securityIdentifier: String? = null,
    val theme: String? = null,
    val visibility: String? = null,
    val onPremisesProvisioningErrors: List<String> = listOf(),
)
