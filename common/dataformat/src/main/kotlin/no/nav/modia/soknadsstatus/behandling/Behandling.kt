package no.nav.modia.soknadsstatus.behandling

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
sealed class Behandling {
    abstract val aktoerREF: List<AktoerREF>
    abstract val ansvarligEnhetREF: String
    abstract val applikasjonBehandlingREF: String
    abstract val applikasjonSakREF: String
    abstract val behandlingsID: String
    abstract val behandlingstema: Behandlingstema
    abstract val behandlingstype: Behandlingstype
    abstract val hendelseType: String
    abstract val hendelsesId: String
    abstract val hendelsesTidspunkt: LocalDateTime
    abstract val hendelsesprodusentREF: HendelsesprodusentREF
    abstract val primaerBehandlingREF: PrimaerBehandlingREF?
    abstract val sakstema: Sakstema
    abstract val sekundaerBehandlingREF: List<SekundaerBehandlingREF>
    abstract val styringsinformasjonListe: List<StyringsinformasjonListe>
}
