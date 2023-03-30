package no.nav.modia.soknadsstatus

import io.ktor.http.*
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.common.token_client.client.OnBehalfOfTokenClient
import no.nav.modia.soknadsstatus.pdl.PdlClient
import no.nav.personoversikt.common.ktor.utils.Security.AuthProviderConfig
import no.nav.personoversikt.common.ktor.utils.Security.JwksConfig
import no.nav.personoversikt.common.ktor.utils.Security.TokenLocation
import no.nav.personoversikt.common.logging.Logging.secureLog
import java.net.URL

interface Configuration {
    val azureAd: AuthProviderConfig
    val oboTokenClient: OnBehalfOfTokenClient
    val machineToMachineTokenClient: MachineToMachineTokenClient
    val repository: soknadsstatusRepository

    companion object {
        fun factory(env: Env): Configuration {
            val oboTokenClient = AzureAdTokenClientBuilder.builder().oboClientFactory(env)
            val machineToMachineTokenClient = AzureAdTokenClientBuilder.builder().machineToMachineClientFactory(env)
            val azureAd = authProviderConfigFactory(env)
            val repository = soknadsstatusRepository(env.datasourceConfiguration.datasource)

            return object : Configuration {
                override val oboTokenClient = oboTokenClient
                override val machineToMachineTokenClient = machineToMachineTokenClient
                override val azureAd = azureAd
                override val repository = repository
            }
        }
    }
}

private fun AzureAdTokenClientBuilder.oboClientFactory(env: Env): OnBehalfOfTokenClient {
    if (env.appMode == AppMode.NAIS) {
        return AzureAdTokenClientBuilder
            .builder()
            .withClientId(env.azureAdConfiguration.clientId)
            .withTokenEndpointUrl(env.azureAdConfiguration.openidConfigTokenEndpoint)
            .withPrivateJwk(env.azureAdConfiguration.appJWK)
            .buildOnBehalfOfTokenClient()
    }

    return object : OnBehalfOfTokenClient {
        init {
            secureLog.warn("Bruker OnBehalfOfTokenClientMock")
        }
        override fun exchangeOnBehalfOfToken(tokenScope: String?, accessToken: String?): String {
            secureLog.info("Bytter token for scope: $tokenScope, med token: $accessToken")
            if (accessToken == null) {
                throw IllegalStateException("Mangler accessToken ved bytte i mock")
            }
            return accessToken
        }
    }
}

private fun AzureAdTokenClientBuilder.machineToMachineClientFactory(env: Env): MachineToMachineTokenClient {
    if (env.appMode == AppMode.NAIS) {
        return AzureAdTokenClientBuilder
            .builder()
            .withClientId(env.azureAdConfiguration.clientId)
            .withTokenEndpointUrl(env.azureAdConfiguration.openidConfigTokenEndpoint)
            .withPrivateJwk(env.azureAdConfiguration.appJWK)
            .buildMachineToMachineTokenClient()
    }

    return object : MachineToMachineTokenClient {
        init {
            secureLog.warn("Bruker MachineToMachineTokenClientMock")
        }

        override fun createMachineToMachineToken(tokenScope: String?): String {
            secureLog.info("Bytter token for scope: $tokenScope")
            return "api:scope:mock"
        }
    }
}

private fun authProviderConfigFactory(env: Env): AuthProviderConfig {
    if (env.appMode == AppMode.NAIS) {
        return AuthProviderConfig(
            name = AzureAD,
            jwksConfig = JwksConfig.OidcWellKnownUrl(env.azureAdConfiguration.wellKnownUrl),
            tokenLocations = listOf(
                TokenLocation.Header(HttpHeaders.Authorization)
            )
        )
    }

    return AuthProviderConfig(
        name = AzureAD,
        jwksConfig = JwksConfig.JwksUrl(env.azureAdConfiguration.openidConfigJWKSUri, env.azureAdConfiguration.openidConfigIssuer),
        tokenLocations = listOf(
            TokenLocation.Header(HttpHeaders.Authorization)
        )
    )
}

// private fun AccessControl.factory(env: Env): AccessControl {
//
// }
