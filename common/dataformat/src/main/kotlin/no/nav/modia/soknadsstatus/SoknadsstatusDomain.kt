package no.nav.modia.soknadsstatus

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import no.nav.api.generated.pdl.enums.IdentGruppe
import no.nav.modia.soknadsstatus.behandling.*

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
        BEHANDLING_OPPRETTET_OG_AVSLUTTET;

        companion object {
            fun convertFromString(type: String): HendelseType {
                return HendelseType.values().first { it.name.lowercase() == type.lowercase() }
            }
        }
    }


    @Serializable
    data class SoknadsstatusOppdatering(
        val ident: String,
        val behandlingsId: String,
        val systemRef: String,
        val tema: String,
        val status: Status,
        val tidspunkt: Instant,
    ) {
        private val systemUrl = mapOf<String, String>()
        fun url() = "https://${systemUrl[systemRef]}/$behandlingsId"
    }

    @Serializable
    data class SoknadsstatusInnkommendeOppdatering(
        val aktorIder: List<String>? = null,
        val identer: List<IdentType>? = null,
        val behandlingsId: String,
        val systemRef: String,
        val tema: String,
        val status: Status,
        val tidspunkt: Instant,
        val hendelse: Hendelse? = null
    ) {
        init {
            if (aktorIder == null && identer == null) {
                throw IllegalArgumentException("Enten aktorIder eller identer må være satt")
            }
        }
    }

    @Serializable
    data class IdentType(
        val ident: String,
        val type: IdentGruppe,
    )

    @Serializable
    data class Soknadsstatuser(
        val identer: List<String>,
        val tema: Map<String, Soknadsstatus>,
    )

    @Serializable
    data class Soknadsstatus(
        var underBehandling: Int = 0,
        var ferdigBehandlet: Int = 0,
        var avbrutt: Int = 0,
    )

    @Serializable
    data class BehandlingDAO(
        val id: String? = null,
        val behandlingId: String? = null,
        val produsentSystem: String? = null,
        val startTidspunkt: LocalDateTime? = null,
        val sluttTidspunkt: LocalDateTime? = null,
        val sistOppdatert: LocalDateTime? = null,
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
        val hendelseId: String? = null,
        val behandlingId: String? = null,
        val hendelseProdusent: String? = null,
        val hendelseTidspunkt: LocalDateTime? = null,
        val hendelseType: HendelseType? = null,
        val status: Status? = null,
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
        val hendelser: List<HendelseDTO>? = null
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
        val produsentSystem: String,
    )
}
