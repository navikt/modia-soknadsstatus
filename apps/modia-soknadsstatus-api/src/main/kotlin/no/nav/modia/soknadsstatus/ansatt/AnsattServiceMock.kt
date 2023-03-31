package no.nav.modia.soknadsstatus.ansatt

import no.nav.common.types.identer.NavIdent
import no.nav.modia.soknadsstatus.MockData
import no.nav.modia.soknadsstatus.accesscontrol.kabac.RolleListe
import no.nav.modia.soknadsstatus.ansatt.domain.AnsattEnhet

class AnsattServiceMock : AnsattService {
    override fun hentEnhetsliste(ident: NavIdent): List<AnsattEnhet> = listOf(
        AnsattEnhet(
            MockData.veileder.enhetId,
            MockData.veileder.enhetNavn,
            "AKTIV"
        )
    )

    override fun hentVeilederRoller(ident: NavIdent): RolleListe = RolleListe(MockData.veileder.roller)

    override fun hentAnsattFagomrader(ident: String, enhet: String): Set<String> = setOf(MockData.veileder.fagområder)
}