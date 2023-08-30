package no.nav.modia.soknadsstatus

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import no.nav.api.generated.pdl.enums.IdentGruppe
import no.nav.modia.soknadsstatus.behandling.Hendelse
import no.nav.modia.soknadsstatus.behandling.BehandlingAvsluttet
import no.nav.modia.soknadsstatus.behandling.BehandlingOpprettet

object Transformer {
    @JvmStatic
    fun behandlingsStatus(hendelse: Hendelse, mapper: AvslutningsStatusMapper): SoknadsstatusDomain.Status {
        if (hendelse is BehandlingOpprettet) {
            return SoknadsstatusDomain.Status.UNDER_BEHANDLING
        } else if (hendelse is BehandlingAvsluttet) {
            return mapper.getAvslutningsstatus(hendelse.avslutningsstatus.value.lowercase())
        } else {
            throw IllegalArgumentException("Mottok ukjent behandlingstype $hendelse")
        }
    }

    @JvmStatic
    fun transform(
        behandling: Behandling,
        statusMapper: AvslutningsStatusMapper,
        identer: List<SoknadsstatusDomain.IdentType>,
    ): SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering {
        return SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering(
            identer = identer,
            tema = behandling.sakstema.value,
            behandlingsId = behandling.behandlingsID,
            systemRef = behandling.hendelsesprodusentREF.value,
            status = behandlingsStatus(behandling, statusMapper),
            tidspunkt = behandling.hendelsesTidspunkt.toInstant(TimeZone.currentSystemDefault()),
        )
    }
}
