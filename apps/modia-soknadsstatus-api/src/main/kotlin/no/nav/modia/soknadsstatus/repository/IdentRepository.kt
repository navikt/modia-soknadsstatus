package no.nav.modia.soknadsstatus.repository

import kotlinx.serialization.Serializable
import no.nav.modia.soknadsstatus.SqlDsl.execute
import no.nav.modia.soknadsstatus.SqlDsl.executeWithResult
import java.sql.Connection
import javax.sql.DataSource

interface IdentRepository : TransactionRepository {
    suspend fun upsert(connection: Connection, ident: IdentDAO): IdentDAO?
}

@Serializable
data class IdentDAO(
    val id: String? = null,
    val ident: String? = null,
)

class IdentRepositoryImpl(dataSource: DataSource) : IdentRepository, TransactionRepositoryImpl(dataSource) {
    object Tabell {
        override fun toString(): String = "identer"
        const val id = "id"
        const val ident = "ident"
    }

    override suspend fun upsert(connection: Connection, ident: IdentDAO): IdentDAO? {
        return connection.executeWithResult(
            """
               INSERT INTO ${Tabell}(${Tabell.ident})
               VALUES (?)
               ON CONFLICT DO NOTHING
               RETURNING *;
           """.trimIndent(),
            ident.ident
        ) {
            IdentDAO(
                id = it.getString(Tabell.id),
                ident = it.getString(Tabell.ident)
            )
        }.firstOrNull()
    }
}