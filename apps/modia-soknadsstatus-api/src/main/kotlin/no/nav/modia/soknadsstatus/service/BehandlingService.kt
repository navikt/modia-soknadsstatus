package no.nav.modia.soknadsstatus.service

import no.nav.modia.soknadsstatus.SoknadsstatusDomain
import no.nav.modia.soknadsstatus.behandling.Hendelse
import no.nav.modia.soknadsstatus.repository.BehandlingRepository
import java.sql.Connection

interface BehandlingService {
    suspend fun upsert(
        connection: Connection? = null,
        behandling: SoknadsstatusDomain.BehandlingDAO,
    ): SoknadsstatusDomain.BehandlingDAO?

    suspend fun getAllForIdent(idents: List<String>): List<SoknadsstatusDomain.BehandlingDAO>
}

class BehandlingServiceImpl(private val behandlingRepository: BehandlingRepository) : BehandlingService {
    override suspend fun upsert(
        connection: Connection?,
        behandling: SoknadsstatusDomain.BehandlingDAO,
    ): SoknadsstatusDomain.BehandlingDAO? {
        return behandlingRepository.useTransactionConnection(connection) {
            behandlingRepository.upsert(
                it, behandling
            )
        }
    }

    override suspend fun getAllForIdent(idents: List<String>): List<SoknadsstatusDomain.BehandlingDAO> {
        return behandlingRepository.getByIdents(idents.toTypedArray())
    }
}