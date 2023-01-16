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
    data class Soknadstatus(
        val fnr: String,
        val tema: String,
        val status: Status,
        val tidspunkt: Instant
    )
}
