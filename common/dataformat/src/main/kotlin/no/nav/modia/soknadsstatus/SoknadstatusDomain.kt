package no.nav.modia.soknadsstatus

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
object soknadsstatusDomain {
    @Serializable
    enum class Status {
        UNDER_BEHANDLING,
        FERDIG_BEHANDLET,
        AVBRUTT,
    }

    @Serializable
    data class soknadsstatusOppdatering(
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
    data class soknadsstatusInnkommendeOppdatering(
        val aktorIder: List<String>,
        val behandlingsRef: String,
        val systemRef: String,
        val tema: String,
        val status: Status,
        val tidspunkt: Instant,
    )

    @Serializable
    data class soknadsstatuser(
        val ident: String,
        val tema: Map<String, soknadsstatus>
    )

    @Serializable
    data class soknadsstatus(
        var underBehandling: Int = 0,
        var ferdigBehandlet: Int = 0,
        var avbrutt: Int = 0,
    )
}
