package no.nav.modia.soknadsstatus.ansatt

import no.nav.common.client.axsys.AxsysClient
import no.nav.modia.soknadsstatus.AppMode
import no.nav.modia.soknadsstatus.azure.MSGraphService

object AnsattConfig {
    fun factory(
        appMode: AppMode,
        axsys: AxsysClient,
        azureADService: MSGraphService,
        sensitiveTilgangsRoller: SensitiveTilgangsRoller,
        geografiskeTilgangsRoller: GeografiskeTilgangsRoller,
    ): AnsattService {
        if (appMode == AppMode.NAIS) {
            return AnsattServiceImpl(axsys, azureADService, sensitiveTilgangsRoller, geografiskeTilgangsRoller)
        }
        return AnsattServiceMock()
    }
}
