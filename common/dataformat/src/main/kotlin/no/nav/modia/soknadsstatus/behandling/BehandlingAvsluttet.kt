package no.nav.modia.soknadsstatus.behandling

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class BehandlingAvsluttet(
    override val aktoerREF: List<AktoerREF>,
    override val identREF: List<IdentREF> = listOf(),
    override val ansvarligEnhetREF: String? = null,
    override val applikasjonBehandlingREF: String? = null,
    override val applikasjonSakREF: String? = null,
    override val behandlingsID: String,
    override val behandlingstema: Behandlingstema,
    override val behandlingstype: Behandlingstype,
    override val hendelseType: String,
    override val hendelsesId: String,
    override val hendelsesTidspunkt: LocalDateTime,
    override val hendelsesprodusentREF: HendelsesprodusentREF,
    override val primaerBehandlingREF: PrimaerBehandlingREF? = null,
    override val sakstema: Sakstema,
    override val sekundaerBehandlingREF: List<SekundaerBehandlingREF> = listOf(),
    override val styringsinformasjonListe: List<StyringsinformasjonListe> = listOf(),
    override val avslutningsstatus: Avslutningsstatus? = null,
    override val opprettelsesTidspunkt: LocalDateTime? = null,
) : Hendelse()
