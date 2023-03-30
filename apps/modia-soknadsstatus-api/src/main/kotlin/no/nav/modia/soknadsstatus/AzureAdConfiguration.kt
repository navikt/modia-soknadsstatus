package no.nav.modia.soknadsstatus

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import no.nav.common.token_client.utils.env.AzureAdEnvironmentVariables
import no.nav.personoversikt.common.utils.EnvUtils

class AzureAdConfiguration(
    val clientId: String,
    val clientSecret: String,
    val tenantId: String,
    val appJWK: String,
    preAuthorizedApps: String,
    val wellKnownUrl: String,
    val openidConfigIssuer: String,
    val openidConfigJWKSUri: String,
    val openidConfigTokenEndpoint: String,
) {
    val preAuthorizedApps = Json.decodeFromString<List<PreauthorizedApp>>(preAuthorizedApps)

    @Serializable
    class PreauthorizedApp(
        val name: String,
        val clientId: String
    )

    companion object {
        fun load(): AzureAdConfiguration {
            val clientId = EnvUtils.getRequiredConfig(AzureAdEnvironmentVariables.AZURE_APP_CLIENT_ID)
            val clientSecret = EnvUtils.getRequiredConfig(AzureAdEnvironmentVariables.AZURE_APP_CLIENT_SECRET)
            val tenantId = EnvUtils.getRequiredConfig(AzureAdEnvironmentVariables.AZURE_APP_TENANT_ID)
            val appJWK = EnvUtils.getRequiredConfig(AzureAdEnvironmentVariables.AZURE_APP_JWK)
            val preAuthorizedApps =
                EnvUtils.getRequiredConfig(AzureAdEnvironmentVariables.AZURE_APP_PRE_AUTHORIZED_APPS)
            val wellKnownUrl = EnvUtils.getRequiredConfig(AzureAdEnvironmentVariables.AZURE_APP_WELL_KNOWN_URL)
            val openidConfigIssuer =
                EnvUtils.getRequiredConfig(AzureAdEnvironmentVariables.AZURE_OPENID_CONFIG_ISSUER)
            val openidConfigJWKSUri =
                EnvUtils.getRequiredConfig(AzureAdEnvironmentVariables.AZURE_OPENID_CONFIG_JWKS_URI)
            val openidConfigTokenEndpoint =
                EnvUtils.getRequiredConfig(AzureAdEnvironmentVariables.AZURE_OPENID_CONFIG_TOKEN_ENDPOINT)

            return AzureAdConfiguration(
                clientId = clientId,
                clientSecret = clientSecret,
                tenantId = tenantId,
                appJWK = appJWK,
                preAuthorizedApps = preAuthorizedApps,
                wellKnownUrl = wellKnownUrl,
                openidConfigIssuer = openidConfigIssuer,
                openidConfigJWKSUri = openidConfigJWKSUri,
                openidConfigTokenEndpoint = openidConfigTokenEndpoint,
            )
        }
    }
}
