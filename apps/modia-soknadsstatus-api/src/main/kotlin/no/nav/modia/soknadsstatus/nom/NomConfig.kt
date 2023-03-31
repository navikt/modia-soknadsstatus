package no.nav.modia.soknadsstatus.nom

import no.nav.common.client.nom.NomClient
import no.nav.common.client.nom.NomClientImpl
import no.nav.common.rest.client.RestClient
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.modia.soknadsstatus.AppMode
import no.nav.modiapersonoversikt.consumer.nom.NullCachingNomClient
import no.nav.utils.LoggingInterceptor
import no.nav.utils.getCallId
import okhttp3.OkHttpClient

object NomConfig {
    fun factory(appMode: AppMode, nomEnv: NomEnv, tokenProvider: MachineToMachineTokenClient): NomClient {
        if (appMode == AppMode.NAIS) {
            val scope = nomEnv.scope
            val url = nomEnv.url
            val httpClient: OkHttpClient = RestClient.baseClient()
                .newBuilder()
                .addInterceptor(
                    LoggingInterceptor("Nom") {
                        getCallId()
                    }
                )
                .build()

            val tokenSupplier = { tokenProvider.createMachineToMachineToken(scope) }
            return NullCachingNomClient(NomClientImpl(url, tokenSupplier, httpClient))
        }
        return NomClientMock()
    }
}

data class NomEnv(
    val scope: String,
    val url: String,
)
