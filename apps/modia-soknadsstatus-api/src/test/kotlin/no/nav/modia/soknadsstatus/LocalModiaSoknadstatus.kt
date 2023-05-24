package no.nav.modia.soknadsstatus

import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.common.token_client.utils.env.AzureAdEnvironmentVariables
import java.util.*

fun main() {
    System.setProperty("APP_MODE", "LOCALLY_WITHIN_IDEA")
    System.setProperty("APP_NAME", "modia-soknadsstatus-api")
    System.setProperty("APP_VERSION", "dev")

    System.setProperty("KAFKA_SOURCE_TOPIC", "modia-soknadsstatus")
    System.setProperty("KAFKA_BROKER_URL", "localhost:9092")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_TOPIC", "modia-soknadsstatus-dlq")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_CONSUMER_POLL_INTERVAL_MS", "60000")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_SKIP_TABLE_NAME", "modia_soknadsstatus_dlq_event_skip")
    System.setProperty("KAFKA_DEAD_LETTER_QUEUE_METRICS_GAUGE_NAME", "modia_soknadsstatus_api_dlq_gauge")

    System.setProperty("JDBC_URL", "jdbc:postgresql://localhost:5432/modia-soknadsstatus")
    System.setProperty("JDBC_USERNAME", "admin")
    System.setProperty("JDBC_PASSWORD", "admin")
    System.setProperty("NAIS_CLUSTER_NAME", "dev-gcp")
    setupAzureAdLocally()
    setUpMocks()

    runApp(port = 9012, useMock = true)
}

private fun setupAzureAdLocally() {
    val jwk = RSAKeyGenerator(2048)
        .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
        .keyID(UUID.randomUUID().toString()) // give the key a unique ID
        .generate()
        .toJSONString()
    val preauthApps = Json.Default.encodeToString(
        listOf(
            AzureAdConfiguration.PreauthorizedApp(name = "other-app", clientId = "some-random-id"),
            AzureAdConfiguration.PreauthorizedApp(name = "another-app", clientId = "another-random-id"),
        )
    )

    System.setProperty(AzureAdEnvironmentVariables.AZURE_APP_TENANT_ID, "tenant")
    System.setProperty(AzureAdEnvironmentVariables.AZURE_APP_CLIENT_ID, "foo")
    System.setProperty(AzureAdEnvironmentVariables.AZURE_APP_CLIENT_SECRET, "app-client-secret")
    System.setProperty(AzureAdEnvironmentVariables.AZURE_APP_JWK, jwk)
    System.setProperty(AzureAdEnvironmentVariables.AZURE_APP_PRE_AUTHORIZED_APPS, preauthApps)
    System.setProperty(
        AzureAdEnvironmentVariables.AZURE_APP_WELL_KNOWN_URL,
        "http://localhost:9015/azuread/.well-known/openid-configuration"
    )
    System.setProperty(AzureAdEnvironmentVariables.AZURE_OPENID_CONFIG_ISSUER, "azuread")
    System.setProperty(
        AzureAdEnvironmentVariables.AZURE_OPENID_CONFIG_JWKS_URI,
        "http://localhost:9015/azuread/.well-known/jwks.json"
    )
    System.setProperty(
        AzureAdEnvironmentVariables.AZURE_OPENID_CONFIG_TOKEN_ENDPOINT,
        "http://localhost:9015/azuread/oauth/token"
    )
}

private fun setUpMocks() {
    val mockEnvs = listOf(
        MockEnv("PDL_API_URL", "https://pdl-api-url.no"),
        MockEnv("PDL_SCOPE", "test:pdl:scope"),
        MockEnv("AXSYS_SCOPE", "test:axsys:scope"),
        MockEnv("AXSYS_URL", "AXSYS_URL"),
        MockEnv("LDAP_URL", "http://ldap-api-url.no"),
        MockEnv("LDAP_USERNAME", "ldap_username"),
        MockEnv("LDAP_PASSWORD", "ldap_password"),
        MockEnv("LDAP_BASEDN", "ldap_basedn"),
        MockEnv("NOM_SCOPE", "test:nom:scope"),
        MockEnv("NOM_URL", "https://nom-api-url.no"),
        MockEnv("NORG2_BASEURL", "https://norg2-api-url.no"),
        MockEnv("SKJERMEDE_PERSONER_PIP_URL", "https://skjermede-personer-api-url.no"),
        MockEnv("SKJERMEDE_PERSONER_SCOPE", "test:skjermede-personer:scope"),
    )

    mockEnvs.forEach {
        System.setProperty(it.key, it.value)
    }
}

private data class MockEnv(
    val key: String,
    val value: String
)
