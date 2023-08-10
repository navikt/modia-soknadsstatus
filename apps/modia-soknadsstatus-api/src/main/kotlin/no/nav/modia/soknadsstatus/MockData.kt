package no.nav.modia.soknadsstatus

import no.nav.common.types.identer.EnhetId
import no.nav.modia.soknadsstatus.ansatt.GeografiskeTilgangsRoller
import no.nav.modia.soknadsstatus.ansatt.RolleListe
import no.nav.modia.soknadsstatus.ansatt.SensitiveTilgangsRoller
import no.nav.modia.soknadsstatus.kafka.AppCluster
import no.nav.modia.soknadsstatus.norg.NorgDomain

object MockData {
    private val sensitiveTilgangsRoller = SensitiveTilgangsRoller(appCluster = AppCluster.LOCALLY)
    private val geografiskeTilgangsRoller = GeografiskeTilgangsRoller(appCluster = AppCluster.LOCALLY)

    object veileder {
        val enhetId = "2990"
        val enhetNavn = "IT Avdelingen"
        val enhet = NorgDomain.Enhet(
            enhetId,
            enhetNavn,
            NorgDomain.EnhetStatus.AKTIV,
            true,
        )
        val navIdent = "Z999999"
        val roller = RolleListe(
            sensitiveTilgangsRoller.kode6,
            sensitiveTilgangsRoller.kode7,
            sensitiveTilgangsRoller.skjermedePersoner,
        ).apply {
            addAll(
                geografiskeTilgangsRoller.regionaleTilgangsRoller,
            )
            addAll(geografiskeTilgangsRoller.nasjonaleTilgangsRoller)
        }

        val axsysEnhet = EnhetId(enhetId)
        val fagområder = "AAP"
    }

    object bruker {
        val fnr = "1010800398"
        val aktorId = "1010800398"
        val geografiskTilknyttning = "2990"
    }
}
