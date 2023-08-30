package no.nav.modia.soknadsstatus.service

import no.nav.modia.soknadsstatus.repository.IdentDAO
import no.nav.modia.soknadsstatus.repository.IdentRepository
import java.sql.Connection

interface IdentService {
    suspend fun upsert(connection: Connection?, ident: IdentDAO): IdentDAO?
}

class IdentServiceImpl(private val identRepository: IdentRepository) : IdentService {
    override suspend fun upsert(connection: Connection?, ident: IdentDAO): IdentDAO? {
        return identRepository.useTransactionConnection {
            identRepository.upsert(it, ident)
        }
    }
}