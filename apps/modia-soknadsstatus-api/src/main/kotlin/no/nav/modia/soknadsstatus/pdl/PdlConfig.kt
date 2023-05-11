package no.nav.modia.soknadsstatus.pdl

import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.common.token_client.client.OnBehalfOfTokenClient
import no.nav.modia.soknadsstatus.AppMode
import no.nav.modia.soknadsstatus.utils.*
import java.net.URL

object PdlConfig {
    fun factory(
        appMode: AppMode,
        pdlEnv: PdlEnv,
        oboTokenProvider: OnBehalfOfTokenClient,
        machineTokenClient: MachineToMachineTokenClient
    ): PdlOppslagService {
        if (appMode == AppMode.NAIS) {
            val scope = pdlEnv.scope
            val url = pdlEnv.url
            val pdlClient = PdlClient(URL(url))

            return PdlOppslagServiceImpl(
                pdlClient,
                oboTokenProvider.bindTo(DownstreamApi.parse(scope)),
                machineTokenClient.bindTo((DownstreamApi.parse(scope)))
            )
        }

        return PdlOppslagServiceMock()
    }
}

data class PdlEnv(
    val url: String,
    val scope: String
)
