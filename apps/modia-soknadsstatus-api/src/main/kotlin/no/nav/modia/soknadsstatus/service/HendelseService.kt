package no.nav.modia.soknadsstatus.service

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
    override suspend fun onNewHendelse(hendelse: Hendelse): Boolean {
        hendelseRepository.useTransactionConnection {
            hendelseRepository.create(it, hendelseToHendelseDAO(hendelse))
            behandlingService.upsert(it, hendelseToBehandlingDAO(hendelse))

        }
    }

    private fun hendelseToHendelseDAO(hendelse: Hendelse): SoknadsstatusDomain.HendelseDAO {
        return SoknadsstatusDomain.HendelseDAO(
            hendelseId = hendelse.hendelsesId,
            behandlingId = hendelse.behandlingsID,
            hendelseProdusent = hendelse.hendelsesprodusentREF.value,
            hendelseType = SoknadsstatusDomain.HendelseType.convertFromMessage(hendelse.hendelseType),
            hendelseTidspunkt = hendelse.hendelsesTidspunkt,
            status = getStatusFromHendelse(hendelse),
            ansvarligEnhet = hendelse.ansvarligEnhetREF,
        )
    }

    private fun getStatusFromHendelse(hendelse: Hendelse): SoknadsstatusDomain.Status {
       return when(hendelse) {
           is BehandlingOpprettet -> SoknadsstatusDomain.Status.UNDER_BEHANDLING
           is BehandlingAvsluttet -> SoknadsstatusDomain.Status.FERDIG_BEHANDLET
       }


        return SoknadsstatusDomain.Status.UNDER_BEHANDLING // TODO handle Infotrygd statuses and avbrutt
    }

    private fun hendelseToBehandlingDAO(hendelse: Hendelse): SoknadsstatusDomain.BehandlingDAO {
        val status = getStatusFromHendelse(hendelse)

        val startTidspunkt =
            if (status == SoknadsstatusDomain.Status.UNDER_BEHANDLING) hendelse.hendelsesTidspunkt else null
        val sluttTidspunkt = if (status != SoknadsstatusDomain.Status.UNDER_BEHANDLING) hendelse.hendelsesTidspunkt else null

        return SoknadsstatusDomain.BehandlingDAO(
            behandlingId = hendelse.behandlingsID,
            produsentSystem = hendelse.hendelsesprodusentREF.value,
            startTidspunkt = startTidspunkt,
            sluttTidspunkt = sluttTidspunkt,
            sistOppdatert = hendelse.hendelsesTidspunkt,
            primaerBehandling = hendelse.primaerBehandlingREF?.behandlingsREF,
            status = status,
            behandlingsTema = hendelse.behandlingstema?.value,
            ansvarligEnhet = hendelse.ansvarligEnhetREF,
            sakstema = hendelse.sakstema.value
        )
    }
}