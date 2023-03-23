package no.nav.modia.soknadsstatus

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import no.nav.modia.soknadsstatus.behandling.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FilterTest {
    @Test
    fun `skal slippe igjennom behandlinger som matcher alle regler`() {
        val result = Filter.filtrerBehandling(generateBehandling().opprettet)
        assertTrue(result)
    }

    @Test
    fun `skal ikke slippe igjennom behandlinger som begynner med ulovlig prefix`() {
        val behandling = generateBehandling(primaerBehandlingREF = PrimaerBehandlingREF("17UlovligPrefix", Type("koderef", "kodeverksref", "type"))).opprettet
        val result = Filter.filtrerBehandling(behandling)
        assertFalse(result)
    }

    @Test
    fun `skal ikke slippe igjennom behandlinger som ikke har primaerBehandlingREF satt`() {
        val behandling = generateBehandling(primaerBehandlingREF = null)
        val result = Filter.filtrerBehandling(behandling.opprettet)
        assertFalse(result)
    }

    @Test
    fun `skal ikke slippe igjennom behandlinger som ikke har godkjent behandlingstype`() {
        val behandling = generateBehandling(behandlingstype = Behandlingstype("koderef", "kodeverksRef", "UGYLDIG TYPE"))
        val result = Filter.filtrerBehandling(behandling.opprettet)
        assertFalse(result)
    }

    @Test
    fun `skal ikke slippe igjennom behandlinger med ulovlig sakstema`() {
        val behandling = generateBehandling(sakstema = Sakstema("koderef", "kodeverksref", "SAK"))
        val result = Filter.filtrerBehandling(behandling.opprettet)
        assertFalse(result)
    }

    @Test
    fun `skal ikke slippe igjennom opprettet behandling om behandlingstype er send 'søknad kvitteringstype'`() {
        val behandling = generateBehandling(behandlingstype = Behandlingstype("koderef", "kodeverksref", "ae0002"))
        val result = Filter.filtrerBehandling(behandling.opprettet)
        assertFalse(result)
    }

    @Test
    fun `skal ikke slippe igjennom opprettet behandling om behandlingstype er 'dokumentinnsending kvitteringstype'`() {
        val behandling = generateBehandling(behandlingstype = Behandlingstype("koderef", "kodeverksref", "ae0001"))
        val result = Filter.filtrerBehandling(behandling.opprettet)
        assertFalse(result)
    }

    @Test
    fun `skal slippe igjennom avsluttet behandling om avslutningsstatus er 'avsluttet'`() {
        val behandling = generateBehandling(avslutningsstatus = Avslutningsstatus("koderef", "kodeverksRef", "avsluttet"))
        val result = Filter.filtrerBehandling(behandling.avsluttet)
        assertTrue(result)
    }

    @Test
    fun `skal ikke slippe igjennom avsluttet behandling om avslutningsstatus ikke er 'avsluttet'`() {
        val behandling = generateBehandling(avslutningsstatus = Avslutningsstatus("koderef", "kodeverksRef", "opprettet"))
        val result = Filter.filtrerBehandling(behandling.avsluttet)
        assertFalse(result)
    }
}

data class BeggeBehandlinger(
    val opprettet: BehandlingOpprettet,
    val avsluttet: BehandlingAvsluttet
)

private fun generateBehandling(
    avslutningsstatus: Avslutningsstatus = Avslutningsstatus("koderef", "kodeverksref", FilterUtils.AVSLUTTET),
    aktoerREF: List<AktoerREF> = listOf(AktoerREF(aktoerId = "123456789")),
    ansvarligEnhetREF: String = "ansvarligEnhet",
    applikasjonBehandlingREF: String = "applikasjon behandling",
    applikasjonSakREF: String = "applikasjonsak",
    behandlingsID: String = "behandlingsID",
    behandlingstema: Behandlingstema = Behandlingstema("koderef", "kodeverksRef", "behandlingstema"),
    behandlingstype: Behandlingstype = Behandlingstype("koderef", "kodeverksref", "ae0047"),
    hendelseType: String = "hendelsestype",
    hendelsesId: String = "hendelsesId",
    hendelsesTidspunkt: LocalDateTime = java.time.LocalDateTime.now().toKotlinLocalDateTime(),
    hendelsesprodusentREF: HendelsesprodusentREF = HendelsesprodusentREF("koderef", "kodeverksRef", "hendelsesprodusentREF"),
    primaerBehandlingREF: PrimaerBehandlingREF? = PrimaerBehandlingREF("behandlingsRef", Type("koderef", "kodeverksRef", "behandlingstype")),
    sakstema: Sakstema = Sakstema("koderef", "kodeverksRef", "sakstema"),
    sekundaerBehandlingREF: List<SekundaerBehandlingREF> = listOf(),
    styringsinformasjonListe: List<StyringsinformasjonListe> = listOf()
): BeggeBehandlinger {
    return BeggeBehandlinger(
        BehandlingOpprettet(
            aktoerREF,
            ansvarligEnhetREF,
            applikasjonBehandlingREF,
            applikasjonSakREF,
            behandlingsID,
            behandlingstema,
            behandlingstype,
            hendelseType,
            hendelsesId,
            hendelsesTidspunkt,
            hendelsesprodusentREF,
            primaerBehandlingREF,
            sakstema,
            sekundaerBehandlingREF,
            styringsinformasjonListe
        ),
        BehandlingAvsluttet(
            avslutningsstatus,
            aktoerREF,
            ansvarligEnhetREF,
            applikasjonBehandlingREF,
            applikasjonSakREF,
            behandlingsID,
            behandlingstema,
            behandlingstype,
            hendelseType,
            hendelsesId,
            hendelsesTidspunkt,
            hendelsesprodusentREF,
            primaerBehandlingREF,
            sakstema,
            sekundaerBehandlingREF,
            styringsinformasjonListe,
        )
    )
}
