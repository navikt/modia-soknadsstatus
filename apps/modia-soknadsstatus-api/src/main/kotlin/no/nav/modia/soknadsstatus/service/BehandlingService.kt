package no.nav.modia.soknadsstatus.service

import no.nav.modia.soknadsstatus.SoknadsstatusDomain
import no.nav.modia.soknadsstatus.repository.BehandlingRepository

interface BehandlingService {
    suspend fun getAllForIdent(idents: List<String>): Result<List<SoknadsstatusDomain.BehandlingDAO>>
    suspend fun update(
        id: String,
        behandling: SoknadsstatusDomain.BehandlingDAO
    )
}

class BehandlingServiceImpl(private val behandlingRepository: BehandlingRepository) : BehandlingService {
    override suspend fun getAllForIdent(idents: List<String>): Result<List<SoknadsstatusDomain.BehandlingDAO>> {
        return behandlingRepository.getByIdents(idents.toTypedArray())
    }

    override suspend fun update(
        id: String,
        behandling: SoknadsstatusDomain.BehandlingDAO
    ) {
        behandlingRepository.update(id, behandling)
    }

}