import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import no.nav.modia.soknadsstatus.SoknadsstatusDomain
import no.nav.modia.soknadsstatus.behandling.Behandling
import no.nav.modia.soknadsstatus.behandling.BehandlingAvsluttet
import no.nav.modia.soknadsstatus.behandling.BehandlingOpprettet
import no.nav.personoversikt.common.logging.Logging.secureLog

object Transformer {

    @JvmStatic
    fun behandlingsStatus(behandling: Behandling): SoknadsstatusDomain.Status? {
        if (behandling is BehandlingOpprettet) {
            return SoknadsstatusDomain.Status.UNDER_BEHANDLING
        } else if (behandling is BehandlingAvsluttet) {
            return when (behandling.avslutningsstatus.value.lowercase()) {
                "avsluttet", "ok", "ja" -> SoknadsstatusDomain.Status.FERDIG_BEHANDLET
                "avbrutt", "nei", "no" -> SoknadsstatusDomain.Status.AVBRUTT
                else -> {
                    secureLog.error("Ukjent behandlingsstatus mottatt: ${behandling.avslutningsstatus.value}")
                    null
                }
            }
        }
        return null
    }

    @JvmStatic
    fun transform(behandling: Behandling): SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering {
        return SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering(
            aktorIder = behandling.aktoerREF.map { it.aktoerId },
            tema = behandling.sakstema.value,
            behandlingsId = behandling.behandlingsID,
            systemRef = behandling.hendelsesprodusentREF.value,
            status = behandlingsStatus(behandling)!!,
            tidspunkt = behandling.hendelsesTidspunkt.toInstant(TimeZone.currentSystemDefault())
        )
    }
}
