package no.nav.modia.soknadsstatus

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

object SoknadsstatusDomain {
    @Serializable
    enum class Status {
        UNDER_BEHANDLING,
        FERDIG_BEHANDLET,
        AVBRUTT,
    }

    @Serializable
    enum class HendelseType {
        BEHANDLING_OPPRETTET,
        BEHANDLING_AVSLUTTET,
        BEHANDLING_OPPRETTET_OG_AVSLUTTET,
        ;

        companion object {
            fun convertFromString(type: String): HendelseType {
                return HendelseType.values().first { it.name.lowercase() == type.lowercase() }
            }
        }
    }

    @Serializable
    data class BehandlingDAO(
        val id: String? = null,
        val behandlingId: String,
        val produsentSystem: String? = null,
        val startTidspunkt: LocalDateTime? = null,
        val sluttTidspunkt: LocalDateTime? = null,
        val sistOppdatert: LocalDateTime,
        val sakstema: String? = null,
        val behandlingsTema: String? = null,
        val behandlingsType: String? = null,
        val status: Status,
        val ansvarligEnhet: String? = null,
        val primaerBehandling: String? = null,
    )

    @Serializable
    data class HendelseDAO(
        val id: String? = null,
        val hendelseId: String,
        val behandlingId: String,
        val hendelseProdusent: String? = null,
        val hendelseTidspunkt: LocalDateTime,
        val hendelseType: HendelseType,
        val status: Status,
        val ansvarligEnhet: String? = null,
    )

    @Serializable
    data class BehandlingDTO(
        val id: String,
        val behandlingId: String,
        val produsentSystem: String,
        val startTidspunkt: LocalDateTime,
        val sluttTidspunkt: LocalDateTime? = null,
        val sistOppdatert: LocalDateTime,
        val sakstema: String,
        val behandlingsTema: String,
        val status: Status,
        val ansvarligEnhet: String,
        val primaerBehandling: String?,
        val hendelser: List<HendelseDTO>? = null,
    )

    @Serializable
    data class HendelseDTO(
        val id: String,
        val hendelseId: String,
        val behandlingId: String,
        val hendelseProdusent: String,
        val hendelseTidspunkt: LocalDateTime,
        val hendelseType: HendelseType,
        val status: Status,
        val ansvarligEnhet: String,
    )
}
