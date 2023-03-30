package no.nav.modia.soknadsstatus

import no.nav.common.types.identer.EnhetId
import no.nav.modia.soknadsstatus.norg.NorgDomain

object MockData {
    object veileder {
        val enhetId = "2990"
        val enhetNavn = "IT Avdelingen"
        val enhet = NorgDomain.Enhet(
            enhetId,
            enhetNavn,
            NorgDomain.EnhetStatus.AKTIV,
            true
        )
        val navIdent = "Z999999"
        val roller = listOf("0000-GA-GOSYS_NASJONAL", "0000-GA-GOSYS_OPPGAVE_BEHANDLER")
        val axsysEnhet = EnhetId(enhetId)
        val fagområder = "AAP"
    }

    object bruker {
        val fnr = "1010800398"
        val aktorId = "1010800398"
        val geografiskTilknyttning = "2990"
    }
}
