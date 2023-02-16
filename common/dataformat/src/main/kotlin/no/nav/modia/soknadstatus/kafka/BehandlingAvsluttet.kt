package no.nav.modia.soknadstatus.kafka

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class BehandlingAvsluttet(
    val aktoerREF: List<AktoerREF>,
    val ansvarligEnhetREF: String,
    val applikasjonBehandlingREF: String,
    val applikasjonSakREF: String,
    val avslutningsstatus: Avslutningsstatus,
    val behandlingsID: String,
    val behandlingstema: Behandlingstema,
    val behandlingstype: Behandlingstype,
    val hendelseType: String,
    val hendelsesId: String,
    val hendelsesTidspunkt: LocalDateTime,
    val hendelsesprodusentREF: HendelsesprodusentREF,
    val primaerBehandlingREF: PrimaerBehandlingREF?,
    val sakstema: Sakstema,
    val sekundaerBehandlingREF: List<SekundaerBehandlingREF>,
    val styringsinformasjonListe: List<StyringsinformasjonListe>
)
