package no.nav.modia.soknadsstatus.service

import no.nav.modia.soknadsstatus.SoknadsstatusDomain
import no.nav.modia.soknadsstatus.repository.BehandlingRepository

interface BehandlingService {
    suspend fun getAllForIdent(idents: List<String>): Result<List<SoknadsstatusDomain.BehandlingDAO>>
    suspend fun update()
}

class BehandlingServiceImpl(private val behandlingRepository: BehandlingRepository) : BehandlingService {
    override suspend fun getAllForIdent(idents: List<String>): Result<List<SoknadsstatusDomain.BehandlingDAO>> {
        TODO("Not yet implemented")
    }

    override suspend fun update() {
        TODO("Not yet implemented")
    }

}