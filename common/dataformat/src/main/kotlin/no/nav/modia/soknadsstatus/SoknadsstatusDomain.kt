package no.nav.modia.soknadsstatus

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import no.nav.api.generated.pdl.enums.IdentGruppe
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

object SoknadsstatusDomain {
    @Serializable
    enum class Status {
        UNDER_BEHANDLING,
        FERDIG_BEHANDLET,
        AVBRUTT,
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
        val type: IdentGruppe
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
}
