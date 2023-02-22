package no.nav.modia.soknadstatus

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
object SoknadstatusDomain {
    @Serializable
    enum class Status {
        UNDER_BEHANDLING,
        FERDIG_BEHANDLET,
        AVBRUTT,
    }

    @Serializable
    data class SoknadstatusOppdatering(
        val ident: String,
        val behandlingsRef: String,
        val systemRef: String,
        val tema: String,
        val status: Status,
        val tidspunkt: Instant,
    ) {
        private val systemUrl = mapOf<String, String>()
        fun url() = "https://${systemUrl[systemRef]}/$behandlingsRef"
    }

    @Serializable
    data class SoknadstatusInnkommendeOppdatering(
        val identer: List<String>,
        val behandlingsRef: String,
        val systemRef: String,
        val tema: String,
        val status: Status,
        val tidspunkt: Instant,
    )

    @Serializable
    data class Soknadstatuser(
        val ident: String,
        val tema: Map<String, Soknadstatus>
    )

    @Serializable
    data class Soknadstatus(
        var underBehandling: Int = 0,
        var ferdigBehandlet: Int = 0,
        var avbrutt: Int = 0,
    )
}
