package no.nav.modia.soknadsstatus.service

import no.nav.modia.soknadsstatus.SoknadsstatusDomain
import no.nav.modia.soknadsstatus.pdl.PdlOppslagService
import no.nav.modia.soknadsstatus.repository.BehandlingRepository
import java.sql.Connection

interface BehandlingService {
    suspend fun upsert(
        connection: Connection,
        behandling: SoknadsstatusDomain.BehandlingDAO,
    ): SoknadsstatusDomain.BehandlingDAO?

    suspend fun getAllForIdents(idents: List<String>): List<SoknadsstatusDomain.BehandlingDAO>

    suspend fun getAllForIdent(userToken: String, ident: String): List<SoknadsstatusDomain.BehandlingDAO>

    suspend fun getByBehandlingId(behandlingId: String): SoknadsstatusDomain.BehandlingDAO?
}

class BehandlingServiceImpl(private val behandlingRepository: BehandlingRepository, private val pdlOppslagService: PdlOppslagService) : BehandlingService {
    override suspend fun upsert(
        connection: Connection,
        behandling: SoknadsstatusDomain.BehandlingDAO,
    ): SoknadsstatusDomain.BehandlingDAO? {
        return behandlingRepository.useTransactionConnection(connection) {
            behandlingRepository.upsert(
                it,
                behandling,
            )
        }
    }

    override suspend fun getAllForIdents(idents: List<String>): List<SoknadsstatusDomain.BehandlingDAO> {
        return behandlingRepository.getByIdents(idents)
    }

    override suspend fun getAllForIdent(userToken: String, ident: String): List<SoknadsstatusDomain.BehandlingDAO> {
        val idents = pdlOppslagService.hentAktiveIdenter(userToken, ident)
        return getAllForIdents(idents)
    }

    override suspend fun getByBehandlingId(behandlingId: String): SoknadsstatusDomain.BehandlingDAO? {
        return behandlingRepository.getByBehandlingId(behandlingId)
    }
}
