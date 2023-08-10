import no.nav.modia.soknadsstatus.FilterUtils
import no.nav.modia.soknadsstatus.FilterUtils.erKvitteringstype
import no.nav.modia.soknadsstatus.behandling.Behandling
import no.nav.modia.soknadsstatus.behandling.BehandlingAvsluttet
import no.nav.modia.soknadsstatus.behandling.BehandlingOpprettet

object Filter {
    private const val ULOVLIG_PREFIX = "17" // ukjent årsak til dette ulovlige prefixet
    private val ulovligeSakstema = arrayOf("FEI", "SAK", "SAP", "OPP", "YRA", "GEN", "AAR", "KLA", "HEL")
    private val lovligeBehandlingstyper = arrayOf("ae0047", "ae0034", "ae0014", "ae0020", "ae0019", "ae0011", "ae0045")

    private fun harLovligSaksTema(behandling: Behandling) = behandling.sakstema.value !in ulovligeSakstema

    private fun harLovlingStatusPaBehandling(behandling: Behandling): Boolean {
        if (behandling is BehandlingOpprettet) {
            return !erKvitteringstype(behandling.hendelseType)
        } else if (behandling is BehandlingAvsluttet) {
            return behandling.avslutningsstatus.value == FilterUtils.AVSLUTTET
        }
        return false
    }

    private fun harLovligBehandlingsstatus(behandling: Behandling): Boolean {
        if (behandling is BehandlingAvsluttet) {
            return behandling.avslutningsstatus.value !== FilterUtils.AVSLUTTET
        }
        return true
    }

    private fun harLovligPrefix(behandling: Behandling) =
        behandling.primaerBehandlingREF?.behandlingsREF?.startsWith(ULOVLIG_PREFIX)?.not() ?: false

    private fun harPrimaerBehandling(behandling: Behandling) = behandling.primaerBehandlingREF != null

    private fun harLovligBehandlingstype(behandling: Behandling) =
        behandling.behandlingstype?.value in lovligeBehandlingstyper

    @JvmStatic
    fun filtrerBehandling(behandling: Behandling): Boolean {
        val checks = listOf<(behandling: Behandling) -> Boolean>(
            ::harLovligSaksTema,
            ::harPrimaerBehandling,
            ::harLovligPrefix,
            ::harLovligBehandlingstype,
            ::harLovlingStatusPaBehandling,
        )

        for (check in checks) {
            if (!check(behandling)) {
                return false
            }
        }

        return true
    }
}
