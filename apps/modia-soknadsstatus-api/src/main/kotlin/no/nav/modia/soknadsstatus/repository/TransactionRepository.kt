package no.nav.modia.soknadsstatus.repository

import java.sql.Connection
import javax.sql.DataSource

interface TransactionRepository {
    suspend fun <T> useTransactionConnection(
        existingConnection: Connection? = null,
        block: suspend (Connection) -> T
    ): T
}

open class TransactionRepositoryImpl(protected val dataSource: DataSource) : TransactionRepository {
    override suspend fun <T> useTransactionConnection(
        existingConnection: Connection?,
        block: suspend (Connection) -> T
    ): T {
        return if (existingConnection == null) {
            dataSource.connection.useTransactionConnection {
                block(it)
            }
        } else {
            block(existingConnection)
        }
    }
}

private suspend fun <T> Connection.useTransactionConnection(block: suspend (Connection) -> T): T = this.use {
    this.autoCommit = false
    try {
        val result = block(this)
        this.commit()
        this.autoCommit = true
        result
    } catch (e: Exception) {
        this.rollback()
        this.autoCommit = true
        throw e
    }
}