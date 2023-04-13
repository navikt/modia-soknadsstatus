package no.nav.modia.soknadsstatus.ansatt.domain

import kotlinx.serialization.Serializable
import java.util.function.Function

@Serializable
data class AnsattEnhet(
    val enhetId: String,
    val enhetNavn: String,
    val status: String? = null
) {
    fun erAktiv(): Boolean {
        return "AKTIV" == status?.uppercase()
    }

    companion object {
        val TIL_ENHET_ID =
            Function { ansattEnhet: AnsattEnhet -> ansattEnhet.enhetId }
    }
}
