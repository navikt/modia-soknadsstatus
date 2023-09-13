package no.nav.modia.soknadsstatus.repository

import java.sql.Connection
import javax.sql.DataSource

interface TransactionRepository {
    suspend fun <T> useTransactionConnection(
        existingConnection: Connection? = null,
        block: suspend (Connection) -> T,
    ): T

    fun createPreparedVariables(length: Int): String {
        val res = mutableListOf<String>();
        for (i in 0..length) {
            res.add("?")
        }
        return res.joinToString()
    }
}

open class TransactionRepositoryImpl(protected val dataSource: DataSource) : TransactionRepository {
    override suspend fun <T> useTransactionConnection(
        existingConnection: Connection?,
        block: suspend (Connection) -> T,
    ): T {
        return if (existingConnection == null) {
            dataSource.connection.useTransactionConnection {
                block(it)
            }
        } else {
            block(existingConnection)
        }
    }

    override fun createPreparedVariables(length: Int): String {
        val res = mutableListOf<String>();
        for (i in 1..length) {
            res.add("?")
        }
        return res.joinToString()
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
