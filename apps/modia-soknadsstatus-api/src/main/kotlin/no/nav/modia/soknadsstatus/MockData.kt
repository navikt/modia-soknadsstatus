package no.nav.modia.soknadsstatus

import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import kotlinx.serialization.json.Json
import no.nav.common.token_client.utils.env.AzureAdEnvironmentVariables
import java.util.*

object MockData {
    object Bruker {
        val fnr = "1010800398"
        val aktorId = "1010800398"
        val geografiskTilknyttning = "2990"
    }

    fun setupAzureAdLocally() {
        val jwk =
            RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                .keyID(UUID.randomUUID().toString()) // give the key a unique ID
                .generate()
                .toJSONString()
        val preauthApps =
            Json.Default.encodeToString(
                listOf(
                    AzureAdConfiguration.PreauthorizedApp(name = "other-app", clientId = "some-random-id"),
                    AzureAdConfiguration.PreauthorizedApp(name = "another-app", clientId = "another-random-id"),
                ),
            )

        System.setProperty(AzureAdEnvironmentVariables.AZURE_APP_TENANT_ID, "tenant")
        System.setProperty(AzureAdEnvironmentVariables.AZURE_APP_CLIENT_ID, "foo")
        System.setProperty(AzureAdEnvironmentVariables.AZURE_APP_CLIENT_SECRET, "app-client-secret")
        System.setProperty(AzureAdEnvironmentVariables.AZURE_APP_JWK, jwk)
        System.setProperty(AzureAdEnvironmentVariables.AZURE_APP_PRE_AUTHORIZED_APPS, preauthApps)
        System.setProperty(
            AzureAdEnvironmentVariables.AZURE_APP_WELL_KNOWN_URL,
            "http://localhost:9015/azuread/.well-known/openid-configuration",
        )
        System.setProperty(AzureAdEnvironmentVariables.AZURE_OPENID_CONFIG_ISSUER, "azuread")
        System.setProperty(
            AzureAdEnvironmentVariables.AZURE_OPENID_CONFIG_JWKS_URI,
            "http://localhost:9015/azuread/.well-known/jwks.json",
        )
        System.setProperty(
            AzureAdEnvironmentVariables.AZURE_OPENID_CONFIG_TOKEN_ENDPOINT,
            "http://localhost:9015/azuread/oauth/token",
        )
    }

    fun setUpMocks() {
        val mockEnvs =
            listOf(
                MockEnv("PDL_API_URL", "https://pdl-api-url.no"),
                MockEnv("PDL_API_URL_Q1", "https://pdl-api-url.no"),
                MockEnv("PDL_PIP_API_URL", "https://pdl-pip-api-url.no"),
                MockEnv("PDL_SCOPE", "test:pdl:scope"),
                MockEnv("PDL_SCOPE_Q1", "test:pdl:scope"),
                MockEnv("PDL_PIP_SCOPE", "test:pdl-pip:scope"),
                MockEnv("LDAP_URL", "http://ldap-api-url.no"),
                MockEnv("LDAP_USERNAME", "ldap_username"),
                MockEnv("LDAP_PASSWORD", "ldap_password"),
                MockEnv("LDAP_BASEDN", "ldap_basedn"),
            )

        mockEnvs.forEach {
            System.setProperty(it.key, it.value)
        }
    }

    private data class MockEnv(
        val key: String,
        val value: String,
    )
}
