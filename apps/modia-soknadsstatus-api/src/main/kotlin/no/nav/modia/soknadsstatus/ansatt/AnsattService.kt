package no.nav.modia.soknadsstatus.ansatt

import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.NavIdent
import no.nav.modia.soknadsstatus.azure.AzureADService

interface AnsattService {
    fun hentEnhetsliste(
        userToken: String,
        ident: NavIdent,
    ): List<EnhetId>

    fun hentAnsattFagomrader(
        userToken: String,
        ident: String,
    ): Set<String>

    fun hentVeilederRoller(
        userToken: String,
        ident: String,
    ): RolleListe
}

class AnsattServiceImpl(
    private val azureADService: AzureADService,
) : AnsattService {
    override fun hentEnhetsliste(
        userToken: String,
        ident: NavIdent,
    ): List<EnhetId> = azureADService.hentEnheterForVeileder(ident.get(), userToken)

    override fun hentAnsattFagomrader(
        userToken: String,
        ident: String,
    ): Set<String> = azureADService.hentTemaerForVeileder(ident, userToken).toSet()

    override fun hentVeilederRoller(
        userToken: String,
        ident: String,
    ): RolleListe = RolleListe(azureADService.hentRollerForVeileder(ident, userToken))
}
