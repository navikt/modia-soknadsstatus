package no.nav.modia.soknadsstatus.service

import no.nav.modia.soknadsstatus.repository.BehandlingEierDAO
import no.nav.modia.soknadsstatus.repository.BehandlingEiereRepository
import java.sql.Connection

interface BehandlingEierService {
    suspend fun upsert(
        connection: Connection,
        behandlingEier: BehandlingEierDAO,
    ): BehandlingEierDAO?
}

class BehandlingEierServiceImpl(
    private val behandlingEiereRepository: BehandlingEiereRepository,
) : BehandlingEierService {
    override suspend fun upsert(
        connection: Connection,
        behandlingEier: BehandlingEierDAO,
    ): BehandlingEierDAO? = behandlingEiereRepository.upsert(connection, behandlingEier)
}
