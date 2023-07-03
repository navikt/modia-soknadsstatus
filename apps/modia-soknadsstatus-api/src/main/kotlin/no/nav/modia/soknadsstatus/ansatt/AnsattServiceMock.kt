package no.nav.modia.soknadsstatus.ansatt

import no.nav.common.types.identer.AzureObjectId
import no.nav.common.types.identer.NavIdent
import no.nav.modia.soknadsstatus.MockData
import no.nav.modia.soknadsstatus.ansatt.domain.AnsattEnhet

class AnsattServiceMock : AnsattService {
    override fun hentEnhetsliste(ident: NavIdent): List<AnsattEnhet> = listOf(
        AnsattEnhet(
            MockData.veileder.enhetId,
            MockData.veileder.enhetNavn,
            "AKTIV"
        )
    )

    override fun hentAnsattFagomrader(ident: String, enhet: String): Set<String> = setOf(MockData.veileder.fagområder)
    override suspend fun hentVeiledersGeografiskeOgSensitiveRoller(
        ident: NavIdent,
        azureAdId: AzureObjectId
    ) = MockData.veileder.roller
}
