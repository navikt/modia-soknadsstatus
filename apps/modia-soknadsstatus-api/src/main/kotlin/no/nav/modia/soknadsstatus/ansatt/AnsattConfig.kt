package no.nav.modia.soknadsstatus.ansatt

import no.nav.modia.soknadsstatus.AppMode
import no.nav.modia.soknadsstatus.azure.AzureADService

object AnsattConfig {
    fun factory(
        appMode: AppMode,
        azureADService: AzureADService,
    ): AnsattService {
        if (appMode == AppMode.NAIS) {
            return AnsattServiceImpl(azureADService)
        }
        return AnsattServiceMock()
    }
}
