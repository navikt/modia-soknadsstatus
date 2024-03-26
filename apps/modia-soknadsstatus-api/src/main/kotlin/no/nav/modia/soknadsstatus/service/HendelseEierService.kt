package no.nav.modia.soknadsstatus.service

import no.nav.modia.soknadsstatus.DeleteUpdateResult
import no.nav.modia.soknadsstatus.repository.HendelseEierDAO
import no.nav.modia.soknadsstatus.repository.HendelseEierRepository
import java.sql.Connection

interface HendelseEierService {
    suspend fun upsert(
        connection: Connection,
        hendelseEier: HendelseEierDAO,
    ): HendelseEierDAO?

    suspend fun convertAktorToIdent(aktorFnrMapping: List<Pair<String, String>>): DeleteUpdateResult
}

class HendelseEierServiceImpl(
    private val hendelseEierRepository: HendelseEierRepository,
) : HendelseEierService {
    override suspend fun upsert(
        connection: Connection,
        hendelseEier: HendelseEierDAO,
    ): HendelseEierDAO? = hendelseEierRepository.upsert(connection, hendelseEier)

    override suspend fun convertAktorToIdent(aktorFnrMapping: List<Pair<String, String>>) =
        hendelseEierRepository.useTransactionConnection {
            val updateCount = hendelseEierRepository.updateAktorToFnr(it, aktorFnrMapping)

            DeleteUpdateResult(
                deleteCount = 0,
                updateCount,
            )
        }
}
