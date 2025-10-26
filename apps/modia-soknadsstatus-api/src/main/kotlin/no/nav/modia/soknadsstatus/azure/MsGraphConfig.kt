package no.nav.modia.soknadsstatus.azure

import no.nav.common.client.msgraph.CachedMsGraphClient
import no.nav.common.client.msgraph.MsGraphClient
import no.nav.common.client.msgraph.MsGraphHttpClient
import no.nav.modia.soknadsstatus.AppMode

object MsGraphConfig {
    fun factory(
        appMode: AppMode,
        msGraphEnv: MsGraphEnv,
    ): MsGraphClient {
        if (appMode == AppMode.NAIS) {
            return CachedMsGraphClient(MsGraphHttpClient(msGraphEnv.url))
        }
        return MsGraphClientMock()
    }
}
