import no.nav.modia.soknadsstatus.FilterUtils.erKvitteringstype
import no.nav.modia.soknadsstatus.behandling.BehandlingOpprettet
import no.nav.modia.soknadsstatus.behandling.Hendelse

object Filter {
    private const val ULOVLIG_PREFIX = "17" // ukjent årsak til dette ulovlige prefixet
    private val ulovligeSakstema = arrayOf("FEI", "SAK", "SAP", "OPP", "YRA", "GEN", "AAR", "KLA", "HEL")
    private val lovligeBehandlingstyper = arrayOf("ae0047", "ae0034", "ae0014", "ae0020", "ae0019", "ae0011", "ae0045")

    private fun harLovligSaksTema(hendelse: Hendelse) = hendelse.sakstema.value !in ulovligeSakstema

    private fun harLovligKvitteringsType(hendelse: Hendelse): Boolean {
        if (hendelse is BehandlingOpprettet) {
            return !erKvitteringstype(hendelse.hendelseType)
        }
        return true
    }

    private fun harLovligPrefix(hendelse: Hendelse): Boolean {
        val behandlingsRef = hendelse.primaerBehandlingREF?.behandlingsREF
        if (behandlingsRef != null) {
            return !behandlingsRef.startsWith(ULOVLIG_PREFIX)
        }
        return true
    }

    private fun harPrimaerBehandling(hendelse: Hendelse) = hendelse.primaerBehandlingREF != null

    private fun harLovligBehandlingstype(hendelse: Hendelse) = hendelse.behandlingstype?.value in lovligeBehandlingstyper

    @JvmStatic
    fun filtrerBehandling(hendelse: Hendelse): Boolean {
        val checks =
            listOf<(hendelse: Hendelse) -> Boolean>(
                ::harLovligSaksTema,
                ::harPrimaerBehandling,
                ::harLovligPrefix,
                ::harLovligBehandlingstype,
                ::harLovligKvitteringsType,
            )

        for (check in checks) {
            if (!check(hendelse)) {
                return false
            }
        }

        return true
    }
}
