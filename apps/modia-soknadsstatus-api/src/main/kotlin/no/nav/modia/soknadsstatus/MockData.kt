package no.nav.modia.soknadsstatus

import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.common.token_client.utils.env.AzureAdEnvironmentVariables
import no.nav.common.types.identer.EnhetId
import no.nav.modia.soknadsstatus.ansatt.GeografiskeTilgangsRoller
import no.nav.modia.soknadsstatus.ansatt.RolleListe
import no.nav.modia.soknadsstatus.ansatt.SensitiveTilgangsRoller
import no.nav.modia.soknadsstatus.kafka.AppCluster
import no.nav.modia.soknadsstatus.norg.NorgDomain
import java.util.*

object MockData {
    private val sensitiveTilgangsRoller = SensitiveTilgangsRoller(appCluster = AppCluster.LOCALLY)
    private val geografiskeTilgangsRoller = GeografiskeTilgangsRoller(appCluster = AppCluster.LOCALLY)

    object veileder {
        val enhetId = "2990"
        val enhetNavn = "IT Avdelingen"
        val enhet = NorgDomain.Enhet(
            enhetId,
            enhetNavn,
            NorgDomain.EnhetStatus.AKTIV,
            true
        )
        val navIdent = "Z999999"
        val roller = RolleListe(
            sensitiveTilgangsRoller.kode6,
            sensitiveTilgangsRoller.kode7,
            sensitiveTilgangsRoller.skjermedePersoner
        ).apply {
            addAll(
                geografiskeTilgangsRoller.regionaleTilgangsRoller
            )
            addAll(geografiskeTilgangsRoller.nasjonaleTilgangsRoller)
        }

        val axsysEnhet = EnhetId(enhetId)
        val fagområder = "AAP"
    }

    object bruker {
        val fnr = "1010800398"
        val aktorId = "1010800398"
        val geografiskTilknyttning = "2990"
    }
    fun setupAzureAdLocally() {
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

    fun setUpMocks() {
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
            MockEnv("NORG2_URL", "https://norg2-api-url.no"),
            MockEnv("SKJERMEDE_PERSONER_PIP_URL", "https://skjermede-personer-api-url.no"),
            MockEnv("SKJERMEDE_PERSONER_SCOPE", "test:skjermede-personer:scope"),
            MockEnv("MS_GRAPH_URL", "https://graph.microsoft.com/"),
            MockEnv("MS_GRAPH_SCOPE", "https://graph.microsoft.com/.default")
        )

        mockEnvs.forEach {
            System.setProperty(it.key, it.value)
        }
    }

    private data class MockEnv(
        val key: String,
        val value: String
    )
}
