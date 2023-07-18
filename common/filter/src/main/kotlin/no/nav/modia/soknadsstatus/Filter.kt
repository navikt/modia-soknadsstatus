import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingStatus
import no.nav.modia.soknadsstatus.FilterUtils
import no.nav.modia.soknadsstatus.FilterUtils.erKvitteringstype
import no.nav.modia.soknadsstatus.behandling.Behandling
import no.nav.modia.soknadsstatus.behandling.BehandlingAvsluttet
import no.nav.modia.soknadsstatus.behandling.BehandlingOpprettet
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingOpprettet as ArenaInfotrygdBehandlingOpprettet
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingAvsluttet as ArenaInfotrygdBehandlingAvsluttet

object Filter {
    private const val ULOVLIG_PREFIX = "17" // ukjent årsak til dette ulovlige prefixet
    private val ulovligeSakstema = arrayOf("FEI", "SAK", "SAP", "OPP", "YRA", "GEN", "AAR", "KLA", "HEL")
    private val lovligeBehandlingstyper = arrayOf("ae0047", "ae0034", "ae0014", "ae0020", "ae0019", "ae0011", "ae0045")

    private fun <T> harLovligSaksTema(behandling: T): Boolean {
        return when (behandling) {
            is Behandling -> behandling.sakstema.value !in ulovligeSakstema
            is BehandlingStatus -> behandling.sakstema.value !in ulovligeSakstema

            else -> {
                false
            }
        }
    }

    private fun <T> harLovlingStatusPaBehandling(behandling: T): Boolean {
        return when (behandling) {
            is BehandlingOpprettet -> !erKvitteringstype(behandling.hendelseType)
            is BehandlingAvsluttet -> behandling.avslutningsstatus.value == FilterUtils.AVSLUTTET
            is ArenaInfotrygdBehandlingOpprettet -> true
            is ArenaInfotrygdBehandlingAvsluttet -> behandling.avslutningsstatus.value == FilterUtils.AVSLUTTET

            else -> {
                false
            }
        }
    }

    private fun <T> harLovligBehandlingsstatus(behandling: T): Boolean {
        return when (behandling) {
            is BehandlingAvsluttet -> behandling.avslutningsstatus.value !== FilterUtils.AVSLUTTET
            else -> {
                true
            }
        }
    }

    private fun <T> harLovligPrefix(behandling: T): Boolean {
        return when (behandling) {
            is Behandling -> behandling.primaerBehandlingREF?.behandlingsREF?.startsWith(ULOVLIG_PREFIX)?.not() ?: false
            is BehandlingStatus -> behandling.primaerBehandlingREF?.behandlingsREF?.startsWith(ULOVLIG_PREFIX)?.not() ?: false
            else -> {
                false
            }
        }
    }

    private fun <T> harPrimaerBehandling(behandling: T): Boolean {
        return when (behandling) {
            is Behandling -> behandling.primaerBehandlingREF != null
            is BehandlingStatus -> behandling.primaerBehandlingREF != null
            else -> {
                false
            }
        }
    }

    private fun <T> harLovligBehandlingstype(behandling: T): Boolean {
        return when (behandling) {
            is Behandling -> behandling.behandlingstype?.value in lovligeBehandlingstyper
            is BehandlingStatus -> behandling.behandlingstype?.value in lovligeBehandlingstyper
            else -> {
                false
            }
        }
    }

    fun <T> filtrerBehandling(behandling: T): Boolean {
        val checks = listOf<(behandling: T) -> Boolean>(::harLovligSaksTema, ::harPrimaerBehandling, ::harLovligPrefix, ::harLovligBehandlingstype, ::harLovlingStatusPaBehandling)

        for (check in checks) {
            if (!check(behandling)) {
                return false
            }
        }

        return true
    }
}

