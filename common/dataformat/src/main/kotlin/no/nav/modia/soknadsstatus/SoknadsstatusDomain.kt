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
            fun convertFromString(type: String): HendelseType = HendelseType.values().first { it.name.lowercase() == type.lowercase() }

            private val pattern = "(?<=.)[A-Z]".toRegex()

            fun convertFromCamelCase(type: String): HendelseType {
                val snakeCaseType = type.replace(pattern, "_$0").uppercase()
                return HendelseType.values().first { it.name == snakeCaseType }
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
        val primaerBehandlingId: String? = null,
        val primaerBehandlingType: String? = null,
        val applikasjonSak: String? = null,
        val applikasjonBehandling: String? = null,
        val hendelser: List<Hendelse>? = null,
        val sobFlag: Boolean? = false,
    )

    @Serializable
    data class Hendelse(
        val id: String? = null,
        val hendelseId: String,
        val behandlingId: String,
        val modiaBehandlingId: String,
        val hendelseProdusent: String? = null,
        val hendelseTidspunkt: LocalDateTime,
        val behandlingsTema: String? = null,
        val behandlingsType: String? = null,
        val hendelseType: HendelseType,
        val status: Status,
        val ansvarligEnhet: String? = null,
    )

    @Serializable
    data class PrimaerBehandling(
        val behandlingId: String,
        val type: String,
    )
}
