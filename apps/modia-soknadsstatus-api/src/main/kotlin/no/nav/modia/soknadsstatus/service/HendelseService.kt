package no.nav.modia.soknadsstatus.service

import no.nav.modia.soknadsstatus.InnkommendeHendelse
import no.nav.modia.soknadsstatus.SoknadsstatusDomain
import no.nav.modia.soknadsstatus.behandling.BehandlingAvsluttet
import no.nav.modia.soknadsstatus.behandling.BehandlingOpprettet
import no.nav.modia.soknadsstatus.behandling.Hendelse
import no.nav.modia.soknadsstatus.repository.HendelseRepository

interface HendelseService {
    suspend fun onNewHendelse(hendelse: Hendelse): Boolean
}

class HendelseServiceImpl(
    private val hendelseRepository: HendelseRepository,
    private val behandlingService: BehandlingService,
) : HendelseService {
    override suspend fun onNewHendelse(hendelse: InnkommendeHendelse): Boolean {
        hendelseRepository.useTransactionConnection {
            hendelseRepository.create(it, hendelseToHendelseDAO(hendelse))
            behandlingService.upsert(it, hendelseToBehandlingDAO(hendelse))

        }
    }

    private fun hendelseToHendelseDAO(hendelse: InnkommendeHendelse): SoknadsstatusDomain.HendelseDAO {
        return SoknadsstatusDomain.HendelseDAO(
            hendelseId = hendelse.hendelsesId,
            behandlingId = hendelse.behandlingsId,
            hendelseProdusent = hendelse.hendelsesProdusent,
            hendelseType = hendelse.hendelsesType,
            hendelseTidspunkt = hendelse.hendelsesTidspunkt,
            status = hendelse.status,
            ansvarligEnhet = hendelse.ansvarligEnhet,
        )
    }

    private fun hendelseToBehandlingDAO(hendelse: InnkommendeHendelse): SoknadsstatusDomain.BehandlingDAO {
        return SoknadsstatusDomain.BehandlingDAO(
            behandlingId = hendelse.behandlingsId,
            produsentSystem = hendelse.hendelsesProdusent,
            sistOppdatert = hendelse.hendelsesTidspunkt,
            primaerBehandling = hendelse.primaerBehandling,
            status = hendelse.status,
            behandlingsTema = hendelse.behandlingsTema,
            ansvarligEnhet = hendelse.ansvarligEnhet,
            sakstema = hendelse.sakstema,
            behandlingsType = hendelse.behandlingsType
        )
    }
}