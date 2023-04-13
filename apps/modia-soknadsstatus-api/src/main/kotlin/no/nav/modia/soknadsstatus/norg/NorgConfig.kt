package no.nav.modia.soknadsstatus.norg

import no.nav.common.rest.client.RestClient
import no.nav.modia.soknadsstatus.AppMode
import no.nav.utils.LoggingInterceptor
import no.nav.utils.XCorrelationIdInterceptor
import okhttp3.OkHttpClient

object NorgConfig {
    fun factory(appMode: AppMode, env: NorgEnv): NorgApi {
        if (appMode == AppMode.NAIS) {
            val url = env.url
            val httpClient: OkHttpClient = RestClient.baseClient().newBuilder()
                .addInterceptor(XCorrelationIdInterceptor())
                .addInterceptor(
                    LoggingInterceptor("Norg2") { request ->
                        requireNotNull(request.header("X-Correlation-ID")) {
                            "Kall uten \"X-Correlation-ID\" er ikke lov"
                        }
                    }
                )
                .build()

            return NorgApiImpl(url, httpClient)
        }
        return NorgApiMock()
    }
}

data class NorgEnv(
    val url: String
)

