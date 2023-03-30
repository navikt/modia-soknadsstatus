package no.nav.modia.soknadsstatus.ansatt

import no.nav.common.client.axsys.AxsysClient
import no.nav.common.client.nom.NomClient
import no.nav.modia.soknadsstatus.AppMode
import no.nav.modia.soknadsstatus.ldap.LDAPService

object AnsattConfig {
    fun factory(
        appMode: AppMode,
        axsys: AxsysClient,
        nomClient: NomClient,
        ldap: LDAPService
    ): AnsattService {
        if (appMode == AppMode.NAIS) {
            return AnsattServiceImpl(axsys, nomClient, ldap)
        }
        return AnsattServiceMock()
    }
}

