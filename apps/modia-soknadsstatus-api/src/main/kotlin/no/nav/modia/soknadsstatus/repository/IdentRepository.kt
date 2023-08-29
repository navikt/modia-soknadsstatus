package no.nav.modia.soknadsstatus.repository

import no.nav.modia.soknadsstatus.SqlDsl.execute
import java.sql.Connection
import javax.sql.DataSource

interface IdentRepository {
    suspend fun create(connection: Connection, ident: String): Boolean
}

class IdentRepositoryImpl(dataSource: DataSource) : IdentRepository, TransactionRepositoryImpl(dataSource) {
    object Tabell {
        override fun toString(): String = "identer"
        const val ident = "ident"
    }

    override suspend fun create(connection: Connection, ident: String): Boolean {
        return dataSource.execute(
            """
               INSERT INTO ${Tabell}(${Tabell.ident})
               VALUES (?)
           """.trimIndent(),
            ident
        )
    }

}