package no.nav.modia.soknadsstatus

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class InnkommendeBehandling(
    val aktoerId: String,
    val behandlingId: String,
    val produsentSystem: String? = null,
    val startTidspunkt: LocalDateTime,
    val sluttTidspunkt: LocalDateTime? = null,
    val sistOppdatert: LocalDateTime,
    val sakstema: String? = null,
    val behandlingsTema: String? = null,
    val behandlingsType: String? = null,
    val status: String? = null,
    val ansvarligEnhet: String? = null,
    val primaerBehandlingId: String? = null,
    val primaerBehandlingType: String? = null,
    val applikasjonSak: String? = null,
    val applikasjonBehandling: String? = null,
)
