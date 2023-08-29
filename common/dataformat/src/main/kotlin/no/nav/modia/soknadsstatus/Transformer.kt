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
        hendelse: Hendelse,
        identGruppe: IdentGruppe,
        statusMapper: AvslutningsStatusMapper,
    ): SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering {
        return SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering(
            identer = hendelse.aktoerREF.map {
                SoknadsstatusDomain.IdentType(
                    ident = it.aktoerId,
                    identGruppe,
                )
            },
            tema = hendelse.sakstema.value,
            behandlingsId = hendelse.behandlingsID,
            systemRef = hendelse.hendelsesprodusentREF.value,
            status = behandlingsStatus(hendelse, statusMapper),
            tidspunkt = hendelse.hendelsesTidspunkt.toInstant(TimeZone.currentSystemDefault()),
        )
    }
}
