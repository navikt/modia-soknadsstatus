package no.nav.modia.soknadsstatus.tilgangsmaskinen

import TilgangsmaskinenMock
import no.nav.common.rest.client.RestClient
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.modia.soknadsstatus.AppMode
import no.nav.modia.soknadsstatus.utils.DownstreamApi
import no.nav.modia.soknadsstatus.utils.createMachineToMachineToken
import no.nav.utils.AuthorizationInterceptor
import no.nav.utils.LoggingInterceptor
import no.nav.utils.XCorrelationIdInterceptor
import okhttp3.OkHttpClient

object TilgangsmaskinenConfig {
    fun factory(
        appMode: AppMode,
        env: TilgangsmaskinenEnv,
        tokenProvider: MachineToMachineTokenClient,
    ): Tilgangsmaskinen {
        if (appMode == AppMode.NAIS) {
            val scope = env.scope
            val url = env.url

            val httpClient: OkHttpClient =
                RestClient
                    .baseClient()
                    .newBuilder()
                    .addInterceptor(XCorrelationIdInterceptor())
                    .addInterceptor(
                        LoggingInterceptor("TilgangsMaskinen") { request ->
                            requireNotNull(request.header("X-Correlation-ID")) {
                                "Kall uten \"X-Correlation-ID\" er ikke lov"
                            }
                        },
                    ).addInterceptor(
                        AuthorizationInterceptor {
                            tokenProvider.createMachineToMachineToken(scope)
                        },
                    ).build()
            return TilgangsmaskinenImpl(url, httpClient)
        }

        return TilgangsmaskinenMock()
    }
}

data class TilgangsmaskinenEnv(
    val url: String,
    val scope: DownstreamApi,
)
