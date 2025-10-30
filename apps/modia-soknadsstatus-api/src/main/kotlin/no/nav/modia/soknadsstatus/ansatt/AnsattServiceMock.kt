package no.nav.modia.soknadsstatus.ansatt

import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.NavIdent
import no.nav.modia.soknadsstatus.MockData

class AnsattServiceMock : AnsattService {
    override fun hentEnhetsliste(
        userToken: String,
        ident: NavIdent,
    ): List<EnhetId> = listOf(EnhetId(MockData.Veileder.enhetId))

    override fun hentAnsattFagomrader(
        userToken: String,
        ident: String,
    ): Set<String> = setOf(MockData.Veileder.fagområder)

    override fun hentVeilederRoller(
        userToken: String,
        ident: String,
    ) = MockData.Veileder.roller
}
