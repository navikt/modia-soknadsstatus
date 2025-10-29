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
        enhet: String,
    ): Set<String>

    suspend fun hentVeiledersGeografiskeOgSensitiveRoller(
        userToken: String,
        ident: NavIdent,
    ): RolleListe
}

class AnsattServiceImpl(
    private val azureADService: AzureADService,
    private val sensitiveTilgangsRoller: SensitiveTilgangsRoller,
    private val geografiskeTilgangsRoller: GeografiskeTilgangsRoller,
) : AnsattService {
    private val sensitiveOgGeografiskeTilgangsRoller: RolleListe
        get() {
            return RolleListe(
                sensitiveTilgangsRoller.kode6,
                sensitiveTilgangsRoller.kode7,
                sensitiveTilgangsRoller.skjermedePersoner,
            ).apply {
                addAll(geografiskeTilgangsRoller.regionaleTilgangsRoller)
                addAll(geografiskeTilgangsRoller.nasjonaleTilgangsRoller)
            }
        }

    override fun hentEnhetsliste(
        userToken: String,
        ident: NavIdent,
    ): List<EnhetId> = azureADService.hentEnheterForVeileder(ident.get(), userToken)

    override fun hentAnsattFagomrader(
        userToken: String,
        ident: String,
        enhet: String,
    ): Set<String> = azureADService.hentTemaerForVeileder(ident, userToken).toSet()

    override suspend fun hentVeiledersGeografiskeOgSensitiveRoller(
        userToken: String,
        ident: NavIdent,
    ): RolleListe = azureADService.hentIntersectRollerForVeileder(ident.get(), userToken, sensitiveOgGeografiskeTilgangsRoller)
}
