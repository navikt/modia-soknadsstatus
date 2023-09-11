package no.nav.modia.soknadsstatus.service

import no.nav.modia.soknadsstatus.InnkommendeHendelse
import no.nav.modia.soknadsstatus.SoknadsstatusDomain
import no.nav.modia.soknadsstatus.pdl.PdlOppslagService
import no.nav.modia.soknadsstatus.repository.BehandlingEierDAO
import no.nav.modia.soknadsstatus.repository.HendelseEierDAO
import no.nav.modia.soknadsstatus.repository.HendelseRepository

interface HendelseService {
    suspend fun onNewHendelse(innkommendeHendelse: InnkommendeHendelse)

    suspend fun getAllForIdents(idents: List<String>): List<SoknadsstatusDomain.Hendelse>

    suspend fun getAllForIdent(userToken: String, ident: String): List<SoknadsstatusDomain.Hendelse>
}

class HendelseServiceImpl(
    private val pdlOppslagService: PdlOppslagService,
    private val hendelseRepository: HendelseRepository,
    private val behandlingService: BehandlingService,
    private val behandlingEierService: BehandlingEierService,
    private val hendelseEierService: HendelseEierService,
) : HendelseService {
    override suspend fun onNewHendelse(innkommendeHendelse: InnkommendeHendelse) {
        hendelseRepository.useTransactionConnection {
            val behandling = behandlingService.upsert(it, hendelseToBehandlingDAO(innkommendeHendelse))
            if (behandling != null) {
                val hendelse =
                    hendelseRepository.create(
                        it,
                        hendelseToHendelseDAO(requireNotNull(behandling.id), innkommendeHendelse),
                    )

                var identer = innkommendeHendelse.identer
                if (identer.isEmpty()) {
                    identer = fetchIdents(innkommendeHendelse.aktoerer)
                }

                for (ident in identer) {
                    behandlingEierService.upsert(
                        it,
                        BehandlingEierDAO(ident = ident, behandlingId = requireNotNull(behandling.id)),
                    )
                    hendelseEierService.upsert(
                        it,
                        HendelseEierDAO(ident = ident, hendelseId = requireNotNull(hendelse?.id)),
                    )
                }
            }
        }
    }

    override suspend fun getAllForIdent(userToken: String, ident: String): List<SoknadsstatusDomain.Hendelse> {
        val idents = pdlOppslagService.hentAktiveIdenter(userToken, ident)
        return getAllForIdents(idents)
    }

    override suspend fun getAllForIdents(idents: List<String>): List<SoknadsstatusDomain.Hendelse> {
        return hendelseRepository.getByIdents(idents)
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

    private fun hendelseToHendelseDAO(behandlingsId: String, hendelse: InnkommendeHendelse): SoknadsstatusDomain.Hendelse {
        return SoknadsstatusDomain.Hendelse(
            hendelseId = hendelse.hendelsesId,
            behandlingId = behandlingsId,
            hendelseProdusent = hendelse.hendelsesProdusent,
            hendelseType = hendelse.hendelsesType,
            hendelseTidspunkt = hendelse.hendelsesTidspunkt,
            status = hendelse.status,
            ansvarligEnhet = hendelse.ansvarligEnhet,
        )
    }

    private fun hendelseToBehandlingDAO(hendelse: InnkommendeHendelse): SoknadsstatusDomain.Behandling {
        val sluttTidspunkt = if (hendelse.hendelsesType != SoknadsstatusDomain.HendelseType.BEHANDLING_OPPRETTET) {
            hendelse.hendelsesTidspunkt
        } else {
            hendelse.hendelsesTidspunkt
        }

        val startTidspunkt = if (hendelse.hendelsesType == SoknadsstatusDomain.HendelseType.BEHANDLING_OPPRETTET) {
            hendelse.hendelsesTidspunkt
        } else if (hendelse.hendelsesType == SoknadsstatusDomain.HendelseType.BEHANDLING_OPPRETTET_OG_AVSLUTTET) {
            hendelse.opprettelsesTidspunkt
        } else {
            hendelse.hendelsesTidspunkt
        }

        return SoknadsstatusDomain.Behandling(
            behandlingId = hendelse.behandlingsId,
            produsentSystem = hendelse.hendelsesProdusent,
            startTidspunkt = startTidspunkt,
            sluttTidspunkt = sluttTidspunkt,
            sistOppdatert = hendelse.hendelsesTidspunkt,
            primaerBehandling = hendelse.primaerBehandling,
            status = hendelse.status,
            behandlingsTema = hendelse.behandlingsTema,
            ansvarligEnhet = hendelse.ansvarligEnhet,
            sakstema = hendelse.sakstema,
            behandlingsType = hendelse.behandlingsType,
        )
    }
}
