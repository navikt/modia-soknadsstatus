package no.nav.modia.soknadstatus.behandling

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class BehandlingOpprettet(
    override val aktoerREF: List<AktoerREF>,
    override val ansvarligEnhetREF: String,
    override val applikasjonBehandlingREF: String,
    override val applikasjonSakREF: String,
    override val behandlingsID: String,
    override val behandlingstema: Behandlingstema,
    override val behandlingstype: Behandlingstype,
    override val hendelseType: String,
    override val hendelsesId: String,
    override val hendelsesTidspunkt: LocalDateTime,
    override val hendelsesprodusentREF: HendelsesprodusentREF,
    override val primaerBehandlingREF: PrimaerBehandlingREF,
    override val sakstema: Sakstema,
    override val sekundaerBehandlingREF: List<SekundaerBehandlingREF>,
    override val styringsinformasjonListe: List<StyringsinformasjonListe>
) : Behandling()
