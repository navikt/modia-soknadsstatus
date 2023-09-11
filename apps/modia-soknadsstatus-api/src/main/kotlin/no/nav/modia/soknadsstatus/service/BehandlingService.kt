package no.nav.modia.soknadsstatus.service

import no.nav.modia.soknadsstatus.SoknadsstatusDomain
import no.nav.modia.soknadsstatus.pdl.PdlOppslagService
import no.nav.modia.soknadsstatus.repository.BehandlingRepository
import java.sql.Connection

interface BehandlingService {
    suspend fun upsert(
        connection: Connection,
        behandling: SoknadsstatusDomain.Behandling,
    ): SoknadsstatusDomain.Behandling?

    suspend fun getAllForIdents(idents: List<String>): List<SoknadsstatusDomain.Behandling>

    suspend fun getAllForIdent(userToken: String, ident: String): List<SoknadsstatusDomain.Behandling>

    suspend fun getByBehandlingId(behandlingId: String): SoknadsstatusDomain.Behandling?
}

class BehandlingServiceImpl(private val behandlingRepository: BehandlingRepository, private val pdlOppslagService: PdlOppslagService) : BehandlingService {
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
        return behandlingRepository.getByIdents(idents)
    }

    override suspend fun getAllForIdent(userToken: String, ident: String): List<SoknadsstatusDomain.Behandling> {
        val idents = pdlOppslagService.hentAktiveIdenter(userToken, ident)
        return getAllForIdents(idents)
    }

    override suspend fun getByBehandlingId(behandlingId: String): SoknadsstatusDomain.Behandling? {
        return behandlingRepository.getByBehandlingId(behandlingId)
    }
}
