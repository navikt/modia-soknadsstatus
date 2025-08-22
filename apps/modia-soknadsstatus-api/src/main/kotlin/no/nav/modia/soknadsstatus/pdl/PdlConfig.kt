package no.nav.modia.soknadsstatus.pdl

import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.common.token_client.client.OnBehalfOfTokenClient
import no.nav.modia.soknadsstatus.AppMode
import no.nav.modia.soknadsstatus.pdlpip.PdlPipApi
import no.nav.modia.soknadsstatus.utils.*
import no.nav.personoversikt.common.logging.Logging.TEAM_LOGS_MARKER
import no.nav.personoversikt.common.logging.Logging.teamLog
import java.net.URL

object PdlConfig {
    fun factory(
        appMode: AppMode,
        pdlEnv: PdlEnv,
        oboTokenProvider: OnBehalfOfTokenClient,
        machineTokenClient: MachineToMachineTokenClient,
        pdlPipApi: PdlPipApi,
    ): PdlOppslagService {
        if (appMode == AppMode.NAIS) {
            teamLog.info(
                TEAM_LOGS_MARKER,
                "PDL scope: ${DownstreamApi.parse(
                    pdlEnv.scope,
                ).application}, ${DownstreamApi.parse(pdlEnv.scope).cluster}, ${DownstreamApi.parse(pdlEnv.scope).namespace} ",
            )
            val scope = pdlEnv.scope
            val url = pdlEnv.url
            val pdlClient =
                PdlClientImpl(
                    oboTokenProvider.bindTo(DownstreamApi.parse(scope)),
                    machineTokenClient.bindTo((DownstreamApi.parse(scope))),
                    URL(url),
                )

            return PdlOppslagServiceImpl(
                pdlClient,
                pdlPipApi,
            )
        }

        return PdlOppslagServiceMock()
    }
}

data class PdlEnv(
    val url: String,
    val scope: String,
)
