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
    data class Behandling(
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
        val hendelser: List<Hendelse>? = null,
    )

    @Serializable
    data class Hendelse(
        val id: String? = null,
        val hendelseId: String,
        val behandlingId: String,
        val hendelseProdusent: String? = null,
        val hendelseTidspunkt: LocalDateTime,
        val hendelseType: HendelseType,
        val status: Status,
        val ansvarligEnhet: String? = null,
    )
}
