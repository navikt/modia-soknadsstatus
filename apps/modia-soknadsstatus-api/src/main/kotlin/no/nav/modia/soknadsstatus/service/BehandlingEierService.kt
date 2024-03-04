package no.nav.modia.soknadsstatus.service

import no.nav.modia.soknadsstatus.repository.BehandlingEierDAO
import no.nav.modia.soknadsstatus.repository.BehandlingEiereRepository
import java.sql.Connection

interface BehandlingEierService {
    suspend fun upsert(
        connection: Connection,
        behandlingEier: BehandlingEierDAO,
    ): BehandlingEierDAO?

    suspend fun getAktorIdsToConvert(limit: Int): List<String>

    suspend fun convertAktorToIdent(aktorFnrMapping: List<Pair<String, String>>)
}

class BehandlingEierServiceImpl(
    private val behandlingEiereRepository: BehandlingEiereRepository,
) : BehandlingEierService {
    override suspend fun upsert(
        connection: Connection,
        behandlingEier: BehandlingEierDAO,
    ): BehandlingEierDAO? = behandlingEiereRepository.upsert(connection, behandlingEier)

    override suspend fun getAktorIdsToConvert(limit: Int): List<String> =
        behandlingEiereRepository.useTransactionConnection {
            behandlingEiereRepository.getAktorIdsToConvert(it, limit)
        }

    override suspend fun convertAktorToIdent(aktorFnrMapping: List<Pair<String, String>>) {
        behandlingEiereRepository.useTransactionConnection {
            behandlingEiereRepository.updateAktorToFnr(it, aktorFnrMapping)
        }
    }
}
