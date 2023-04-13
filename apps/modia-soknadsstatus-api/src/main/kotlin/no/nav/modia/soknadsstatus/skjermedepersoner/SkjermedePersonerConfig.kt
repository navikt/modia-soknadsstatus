package no.nav.modia.soknadsstatus.skjermedepersoner

import no.nav.common.rest.client.RestClient
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.modia.soknadsstatus.AppMode
import no.nav.utils.AuthorizationInterceptor
import no.nav.utils.LoggingInterceptor
import no.nav.utils.XCorrelationIdInterceptor
import okhttp3.OkHttpClient

object SkjermedePersonerConfig {
    fun factory(
        appMode: AppMode,
        env: SkjermedePersonerEnv,
        tokenProvider: MachineToMachineTokenClient
    ): SkjermedePersonerApi {
        if (appMode == AppMode.NAIS) {
            val scope = env.scope
            val url = env.url

            val httpClient: OkHttpClient = RestClient.baseClient().newBuilder()
                .addInterceptor(XCorrelationIdInterceptor())
                .addInterceptor(
                    LoggingInterceptor("SkjermedePersoner") { request ->
                        requireNotNull(request.header("X-Correlation-ID")) {
                            "Kall uten \"X-Correlation-ID\" er ikke lov"
                        }
                    }
                )
                .addInterceptor(
                    AuthorizationInterceptor {
                        tokenProvider.createMachineToMachineToken(scope)
                    }
                )
                .build()
            return SkjermedePersonerApiImpl(url, httpClient)
        }

        return SkjermedePersonerApiMock()
    }
}

data class SkjermedePersonerEnv(
    val url: String,
    val scope: String
)
