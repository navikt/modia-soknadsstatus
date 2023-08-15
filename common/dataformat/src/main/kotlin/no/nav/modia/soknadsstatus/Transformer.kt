package no.nav.modia.soknadsstatus

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import no.nav.modia.soknadsstatus.behandling.Behandling
import no.nav.modia.soknadsstatus.behandling.BehandlingAvsluttet
import no.nav.modia.soknadsstatus.behandling.BehandlingOpprettet

object Transformer {
    @JvmStatic
    fun behandlingsStatus(behandling: Behandling, mapper: AvslutningsStatusMapper): SoknadsstatusDomain.Status {
        if (behandling is BehandlingOpprettet) {
            return SoknadsstatusDomain.Status.UNDER_BEHANDLING
        } else if (behandling is BehandlingAvsluttet) {
            return mapper.getAvslutningsstatus(behandling.avslutningsstatus.value.lowercase())
        } else throw IllegalArgumentException("Mottok ukjent behandlingstype $behandling")
    }

    @JvmStatic
    fun transform(
        behandling: Behandling,
        mapper: AvslutningsStatusMapper
    ): SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering {
        return SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering(
            aktorIder = behandling.aktoerREF.map { it.aktoerId },
            tema = behandling.sakstema.value,
            behandlingsId = behandling.behandlingsID,
            systemRef = behandling.hendelsesprodusentREF.value,
            status = behandlingsStatus(behandling, mapper),
            tidspunkt = behandling.hendelsesTidspunkt.toInstant(TimeZone.currentSystemDefault()),
        )
    }
}
