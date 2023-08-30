package no.nav.modia.soknadsstatus

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class InnkommendeHendelse(
    val aktoerer: List<String>,
    val identer: List<String>,
    val ansvarligEnhet: String? = null,
    val behandlingId: String,
    val hendelseType: SoknadsstatusDomain.HendelseType,
    val hendelseId: String,
    val hendelseTidspunkt: LocalDateTime,
    val hendelseProdusent: String,
    val behandlingsType: String,
    val status: SoknadsstatusDomain.Status,
    val primaerBehandling: String? = null,
)
