package no.nav.modia.soknadsstatus.axsys

import no.nav.common.client.axsys.AxsysClient
import no.nav.common.client.axsys.AxsysV2ClientImpl
import no.nav.common.client.axsys.CachedAxsysClient
import no.nav.common.rest.client.RestClient
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.modia.soknadsstatus.AppMode
import no.nav.modia.soknadsstatus.utils.DownstreamApi
import no.nav.modia.soknadsstatus.utils.createMachineToMachineToken
import no.nav.modia.soknadsstatus.utils.parse
import no.nav.utils.LoggingInterceptor
import no.nav.utils.getCallId
import okhttp3.OkHttpClient

object AxsysConfig {
    fun factory(
        appMode: AppMode,
        env: AxsysEnv,
        tokenProvider: MachineToMachineTokenClient,
    ): AxsysClient {
        if (appMode == AppMode.NAIS) {
            val url: String = env.url
            val httpClient: OkHttpClient =
                RestClient
                    .baseClient()
                    .newBuilder()
                    .addInterceptor(
                        LoggingInterceptor("Axsys") {
                            // Optimalt sett burde denne hentes fra requesten, men det sendes ikke noe tilsvarende callId til axsys
                            getCallId()
                        },
                    ).build()
            val downstreamApi = DownstreamApi.parse(env.scope)
            val tokenSupplier = {
                tokenProvider.createMachineToMachineToken(downstreamApi)
            }

            return CachedAxsysClient(AxsysV2ClientImpl(url, tokenSupplier, httpClient))
        }
        return AxsysClientMock()
    }
}

data class AxsysEnv(
    val url: String,
    val scope: String,
)
