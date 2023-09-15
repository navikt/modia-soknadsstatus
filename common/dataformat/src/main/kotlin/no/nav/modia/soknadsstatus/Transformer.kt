package no.nav.modia.soknadsstatus

import no.nav.modia.soknadsstatus.behandling.BehandlingAvsluttet
import no.nav.modia.soknadsstatus.behandling.BehandlingOpprettet
import no.nav.modia.soknadsstatus.behandling.Hendelse

object Transformer {
    @JvmStatic
    fun behandlingsStatus(hendelse: Hendelse, mapper: AvslutningsStatusMapper): SoknadsstatusDomain.Status {
        if (hendelse is BehandlingOpprettet) {
            return SoknadsstatusDomain.Status.UNDER_BEHANDLING
        } else if (hendelse is BehandlingAvsluttet) {
            return hendelse.avslutningsstatus?.let { mapper.getAvslutningsstatus(it.value.lowercase()) } ?: SoknadsstatusDomain.Status.FERDIG_BEHANDLET
        } else {
            throw IllegalArgumentException("Mottok ukjent behandlingstype $hendelse")
        }
    }

    @JvmStatic
    fun transform(
        hendelse: Hendelse,
        statusMapper: AvslutningsStatusMapper,
        identer: List<String>,
    ): InnkommendeHendelse {
        return InnkommendeHendelse(
            aktoerer = hendelse.aktoerREF.map { it.aktoerId },
            identer = identer,
            behandlingsId = hendelse.behandlingsID,
            behandlingsTema = hendelse.behandlingstema?.value ?: "",
            behandlingsType = hendelse.behandlingstype?.value ?: "",
            hendelsesId = hendelse.hendelsesId,
            hendelsesProdusent = hendelse.hendelsesprodusentREF.value,
            hendelsesTidspunkt = hendelse.hendelsesTidspunkt,
            opprettelsesTidspunkt = hendelse.opprettelsesTidspunkt,
            hendelsesType = SoknadsstatusDomain.HendelseType.convertFromString(hendelse.hendelseType),
            status = behandlingsStatus(hendelse, statusMapper),
            sakstema = hendelse.sakstema.value,
            ansvarligEnhet = hendelse.ansvarligEnhetREF,
            applikasjonSak = hendelse.applikasjonSakREF,
            applikasjonBehandling = hendelse.applikasjonBehandlingREF,
            primaerBehandling = hendelse.primaerBehandlingREF?.let { SoknadsstatusDomain.PrimaerBehandling(it.behandlingsREF, it.type.value) },
        )
    }
}
