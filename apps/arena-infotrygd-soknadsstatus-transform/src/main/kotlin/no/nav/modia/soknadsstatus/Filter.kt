import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingAvsluttet
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingOpprettet
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingStatus
import no.nav.modia.soknadsstatus.FilterUtils

object Filter {
    private const val ULOVLIG_PREFIX = "17" // ukjent årsak til dette ulovlige prefixet
    private val ulovligeSakstema = arrayOf("FEI", "SAK", "SAP", "OPP", "YRA", "GEN", "AAR", "KLA", "HEL")
    private val lovligeBehandlingstyper = arrayOf("ae0047", "ae0034", "ae0014", "ae0020", "ae0019", "ae0011", "ae0045")

    private fun harLovligSaksTema(behandling: BehandlingStatus) = behandling.sakstema.value !in ulovligeSakstema

    private fun harLovlingStatusPaBehandling(behandling: BehandlingStatus): Boolean {
        if (behandling is BehandlingOpprettet) {
            return true
        } else if (behandling is BehandlingAvsluttet) {
            return behandling.avslutningsstatus.value == FilterUtils.AVSLUTTET
        }

        return false
    }

    private fun harLovligPrefix(behandling: BehandlingStatus) =
        behandling.primaerBehandlingREF?.behandlingsREF?.startsWith(ULOVLIG_PREFIX)?.not() ?: false

    private fun harPrimaerBehandling(behandling: BehandlingStatus) = behandling.primaerBehandlingREF != null

    private fun harLovligBehandlingstype(behandling: BehandlingStatus) =
        behandling.behandlingstype?.value in lovligeBehandlingstyper

    @JvmStatic
    fun filtrerBehandling(behandlingStatus: BehandlingStatus): Boolean {
        val checks = listOf<(behandlingStatus: BehandlingStatus) -> Boolean>(
            ::harLovligSaksTema,
            ::harPrimaerBehandling,
            ::harLovligPrefix,
            ::harLovligBehandlingstype,
            ::harLovlingStatusPaBehandling
        )

        for (check in checks) {
            if (!check(behandlingStatus)) {
                return false
            }
        }

        return true
    }
}
