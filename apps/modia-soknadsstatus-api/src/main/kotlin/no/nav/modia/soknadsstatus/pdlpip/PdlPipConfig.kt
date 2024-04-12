package no.nav.modia.soknadsstatus.pdlpip

import no.nav.common.rest.client.RestClient
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.modia.soknadsstatus.AppMode
import no.nav.modia.soknadsstatus.utils.DownstreamApi
import no.nav.modia.soknadsstatus.utils.createMachineToMachineToken
import no.nav.utils.AuthorizationInterceptor
import no.nav.utils.LoggingInterceptor
import no.nav.utils.XCorrelationIdInterceptor
import okhttp3.OkHttpClient

object PdlPipConfig {
    fun factory(
        appMode: AppMode,
        env: PdlPipApiEnv,
        tokenProvider: MachineToMachineTokenClient,
    ): PdlPipApi {
        if (appMode == AppMode.NAIS) {
            val scope = env.scope
            val url = env.url

            val httpClient: OkHttpClient =
                RestClient
                    .baseClient()
                    .newBuilder()
                    .addInterceptor(XCorrelationIdInterceptor())
                    .addInterceptor(
                        LoggingInterceptor("Pdl-pip-api") { request ->
                            requireNotNull(request.header("X-Correlation-ID")) {
                                "Kall uten \"X-Correlation-ID\" er ikke lov"
                            }
                        },
                    ).addInterceptor(
                        AuthorizationInterceptor {
                            tokenProvider.createMachineToMachineToken(scope)
                        },
                    ).build()
            return PdlPipApiImpl(url, httpClient)
        }

        return PdlPipApiMock()
    }
}

data class PdlPipApiEnv(
    val url: String,
    val scope: DownstreamApi,
)
