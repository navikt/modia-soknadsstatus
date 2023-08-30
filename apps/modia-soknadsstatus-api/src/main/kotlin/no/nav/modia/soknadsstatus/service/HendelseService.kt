package no.nav.modia.soknadsstatus.service

import no.nav.modia.soknadsstatus.InnkommendeHendelse
import no.nav.modia.soknadsstatus.SoknadsstatusDomain
import no.nav.modia.soknadsstatus.pdl.PdlOppslagService
import no.nav.modia.soknadsstatus.repository.BehandlingEierDAO
import no.nav.modia.soknadsstatus.repository.HendelseEierDAO
import no.nav.modia.soknadsstatus.repository.HendelseRepository
import no.nav.modia.soknadsstatus.repository.IdentDAO

interface HendelseService {
    suspend fun onNewHendelse(hendelse: InnkommendeHendelse)
}

class HendelseServiceImpl(
    private val pdlOppslagService: PdlOppslagService,
    private val hendelseRepository: HendelseRepository,
    private val behandlingService: BehandlingService,
    private val identService: IdentService,
    private val behandlingEierService: BehandlingEierService,
    private val hendelseEierService: HendelseEierService,
) : HendelseService {
    override suspend fun onNewHendelse(innkommendeHendelse: InnkommendeHendelse) {
        hendelseRepository.useTransactionConnection {
            val hendelse = hendelseRepository.create(it, hendelseToHendelseDAO(innkommendeHendelse))
            val behandling = behandlingService.upsert(it, hendelseToBehandlingDAO(innkommendeHendelse))
            var identer = innkommendeHendelse.identer
            if (identer.isEmpty()) {
                identer = fetchIdents(innkommendeHendelse.aktoerer)
            }
            for (ident in identer) {
                val owner = identService.upsert(it, IdentDAO(ident = ident))
                behandlingEierService.upsert(
                    it,
                    BehandlingEierDAO(ident = requireNotNull(owner?.id), behandlingId = requireNotNull(behandling?.id))
                )
                hendelseEierService.upsert(
                    it,
                    HendelseEierDAO(ident = requireNotNull(owner?.id), hendelseId = requireNotNull(hendelse?.id))
                )
            }
        }
    }

    private suspend fun fetchIdents(aktoerer: List<String>): List<String> {
        val result = mutableListOf<String>()
        for (aktoer in aktoerer) {
            if (isAktoerId(aktoer)) {
                val ident = pdlOppslagService.hentFnrMedSystemToken(aktoer)
                    ?: throw IllegalArgumentException("Fant ikke ident for aktoer: $aktoer")
                result.add(ident)
            } else {
                result.add(aktoer)
            }
        }
        return result
    }

    private fun isAktoerId(aktoerId: String): Boolean {
        val regex = Regex("^\\d{13}$")

        return aktoerId.matches(regex)
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