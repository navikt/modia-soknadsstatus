package no.nav.modia.soknadsstatus

object FilterUtils {
    const val OPPRETTET = "opprettet"
    const val AVBRUTT = "avbrutt"
    const val AVSLUTTET = "avsluttet"
    const val SEND_SOKNAD_KVITTERINGSTYPE = "ae0002"
    const val DOKUMENTINNSENDING_KVITTERINGSTYPE = "ae0001"
    private const val BEHANDLINGSTATUS_AVSLUTTET = "avsluttet"

    @JvmStatic
    fun erKvitteringstype(type: String): Boolean {
        return type == SEND_SOKNAD_KVITTERINGSTYPE || type == DOKUMENTINNSENDING_KVITTERINGSTYPE
    }
}
