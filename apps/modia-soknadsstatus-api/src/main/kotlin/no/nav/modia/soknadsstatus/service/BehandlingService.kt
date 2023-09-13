package no.nav.modia.soknadsstatus.service

import no.nav.modia.soknadsstatus.SoknadsstatusDomain
import no.nav.modia.soknadsstatus.pdl.PdlOppslagService
import no.nav.modia.soknadsstatus.repository.BehandlingRepository
import java.sql.Connection

interface BehandlingService {
    fun init(hendelseService: HendelseService)
    suspend fun upsert(
        connection: Connection,
        behandling: SoknadsstatusDomain.Behandling,
    ): SoknadsstatusDomain.Behandling?

    suspend fun getAllForIdents(idents: List<String>): List<SoknadsstatusDomain.Behandling>
    suspend fun getAllForIdent(userToken: String, ident: String): List<SoknadsstatusDomain.Behandling>
    suspend fun getByBehandlingId(behandlingId: String): SoknadsstatusDomain.Behandling?
    suspend fun getAllForIdentsWithHendelser(idents: List<String>): List<SoknadsstatusDomain.Behandling>
    suspend fun getAllForIdentWithHendelser(userToken: String, ident: String): List<SoknadsstatusDomain.Behandling>
}

class BehandlingServiceImpl(
    private val behandlingRepository: BehandlingRepository,
    private val pdlOppslagService: PdlOppslagService,
) : BehandlingService {
    private lateinit var hendelseService: HendelseService
    override fun init(hendelseService: HendelseService) {
        this.hendelseService = hendelseService
    }

    override suspend fun upsert(
        connection: Connection,
        behandling: SoknadsstatusDomain.Behandling,
    ): SoknadsstatusDomain.Behandling? {
        return behandlingRepository.useTransactionConnection(connection) {
            behandlingRepository.upsert(
                it,
                behandling,
            )
        }
    }

    override suspend fun getAllForIdents(idents: List<String>): List<SoknadsstatusDomain.Behandling> {
        return behandlingRepository.getByIdents(idents.toTypedArray())
    }

    override suspend fun getAllForIdent(userToken: String, ident: String): List<SoknadsstatusDomain.Behandling> {
        val idents = pdlOppslagService.hentAktiveIdenter(userToken, ident)
        return getAllForIdents(idents)
    }

    override suspend fun getByBehandlingId(behandlingId: String): SoknadsstatusDomain.Behandling? {
        return behandlingRepository.getByBehandlingId(behandlingId)
    }

    override suspend fun getAllForIdentsWithHendelser(idents: List<String>): List<SoknadsstatusDomain.Behandling> {
        val behandlinger = getAllForIdents(idents)
        val hendelser = hendelseService.getAllForIdents(idents)

        return behandlinger.map {
            val behandlingMedHendelser =
                it.copy(hendelser = hendelser.filter { hendelse -> hendelse.behandlingId == requireNotNull(it.id) })
            behandlingMedHendelser
        }
    }

    override suspend fun getAllForIdentWithHendelser(
        userToken: String,
        ident: String
    ): List<SoknadsstatusDomain.Behandling> {
        val idents = pdlOppslagService.hentAktiveIdenter(userToken, ident)
        return getAllForIdentsWithHendelser(idents)
    }
}
