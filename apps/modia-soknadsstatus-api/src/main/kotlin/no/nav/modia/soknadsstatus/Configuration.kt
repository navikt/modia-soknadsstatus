package no.nav.modia.soknadsstatus

import io.ktor.http.*
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.common.token_client.client.OnBehalfOfTokenClient
import no.nav.modia.soknadsstatus.pdl.PdlClient
import no.nav.modia.soknadsstatus.utils.bindTo
import no.nav.personoversikt.common.ktor.utils.Security.AuthProviderConfig
import no.nav.personoversikt.common.ktor.utils.Security.JwksConfig
import no.nav.personoversikt.common.ktor.utils.Security.TokenLocation
import java.net.URL

interface Configuration {
    val pdlClient: PdlClient
    val azureAd: AuthProviderConfig
    val oboTokenClient: OnBehalfOfTokenClient
    val repository: soknadsstatusRepository
}

class ConfigurationImpl(
    env: Env
) : Configuration {
    override val oboTokenClient: OnBehalfOfTokenClient = AzureAdTokenClientBuilder
        .builder()
        .withNaisDefaults()
        .buildOnBehalfOfTokenClient()

    override val azureAd: AuthProviderConfig = AuthProviderConfig(
        name = AzureAD,
        jwksConfig = JwksConfig.OidcWellKnownUrl(env.azureAppWellKnownUrl),
        tokenLocations = listOf(
            TokenLocation.Header(HttpHeaders.Authorization)
        )
    )

    override val pdlClient: PdlClient = PdlClient(url = URL(env.pdlApiUrl), oboTokenProvider = oboTokenClient.bindTo(env.pdlScope))
    override val repository: soknadsstatusRepository = soknadsstatusRepository(env.datasourceConfiguration.datasource)
}
