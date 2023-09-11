package no.nav.modia.soknadsstatus.service

import no.nav.modia.soknadsstatus.repository.HendelseEierDAO
import no.nav.modia.soknadsstatus.repository.HendelseEierRepository
import java.sql.Connection

interface HendelseEierService {
    suspend fun upsert(connection: Connection, hendelseEier: HendelseEierDAO): HendelseEierDAO?
}

class HendelseEierServiceImpl(private val hendelseEierRepository: HendelseEierRepository) : HendelseEierService {
    override suspend fun upsert(connection: Connection, hendelseEier: HendelseEierDAO): HendelseEierDAO? {
        return hendelseEierRepository.upsert(connection, hendelseEier)
    }
}
