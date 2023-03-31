package no.nav.modia.soknadsstatus

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
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
        val aktorIder: List<String>,
        val behandlingsId: String,
        val systemRef: String,
        val tema: String,
        val status: Status,
        val tidspunkt: Instant,
    )

    @Serializable
    data class Soknadsstatuser(
        val ident: String,
        val tema: Map<String, Soknadsstatus>
    )

    @Serializable
    data class Soknadsstatus(
        var underBehandling: Int = 0,
        var ferdigBehandlet: Int = 0,
        var avbrutt: Int = 0,
    )
}
