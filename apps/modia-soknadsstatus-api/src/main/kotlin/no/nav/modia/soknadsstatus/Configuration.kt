package no.nav.modia.soknadsstatus

import io.ktor.http.*
import no.nav.modia.soknadsstatus.pdl.PdlOppslagService
import no.nav.modia.soknadsstatus.pdl.PdlOppslagServiceImpl
import no.nav.modia.soknadsstatus.pdl.PdlOppslagServiceTestImpl
import no.nav.personoversikt.common.ktor.utils.Security
import no.nav.personoversikt.common.utils.EnvUtils

private val defaultValues = mapOf(
    "NAIS_CLUSTER_NAME" to "local",
//    "DATABASE_JDBC_URL" to "jdbc:h2:mem:modiapersonoversikt-innstillinger;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
//                "DATABASE_JDBC_URL" to "jdbc:h2:tcp://localhost:8090/./modiapersonoversikt-innstillinger",
//                "DATABASE_JDBC_URL" to "jdbc:postgresql://localhost:5432/modiapersonoversikt-innstillinger",
    "VAULT_MOUNTPATH" to ""
)

data class Configuration(
    val appname: String = EnvUtils.getRequiredConfig("APP_NAME"),
    val appversion: String = EnvUtils.getRequiredConfig("APP_VERSION"),
    val brokerUrl: String = EnvUtils.getRequiredConfig("KAFKA_BROKER_URL"),
    val sourceTopic: String = EnvUtils.getRequiredConfig("KAFKA_SOURCE_TOPIC"),
    val pdlOppslagService: PdlOppslagService = createPdlOppslagService(),
    val clusterName: String = EnvUtils.getRequiredConfig("NAIS_CLUSTER_NAME", defaultValues),
    val azuread: Security.AuthProviderConfig? =
        EnvUtils.getConfig("AZURE_APP_WELL_KNOWN_URL", defaultValues)?.let { jwksurl ->
            Security.AuthProviderConfig(
                name = AzureAD,
                jwksConfig = Security.JwksConfig.OidcWellKnownUrl(jwksurl),
                tokenLocations = listOf(
                    Security.TokenLocation.Header(HttpHeaders.Authorization)
                )
            )
        },
    val datasourceConfiguration: DatasourceConfiguration = DatasourceConfiguration()
)

private fun createPdlOppslagService(): PdlOppslagService {
    val pdlApiUrl = EnvUtils.getConfig("PDL_API_URL")
    return if (pdlApiUrl != null) {
        PdlOppslagServiceImpl()
    } else {
        PdlOppslagServiceTestImpl()
    }
}