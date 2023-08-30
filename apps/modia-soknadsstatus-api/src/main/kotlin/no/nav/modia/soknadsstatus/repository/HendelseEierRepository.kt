package no.nav.modia.soknadsstatus.repository

import kotlinx.serialization.Serializable
import no.nav.modia.soknadsstatus.SqlDsl.executeWithResult
import java.sql.Connection
import javax.sql.DataSource

interface HendelseEierRepository : TransactionRepository {
    fun upsert(connection: Connection, hendelseEier: HendelseEierDAO): HendelseEierDAO?
}

@Serializable
data class HendelseEierDAO(
    val id: String? = null,
    val ident: String? = null,
    val hendelseId: String? = null,
)

class HendelseEierRepositoryImpl(dataSource: DataSource) : HendelseEierRepository,
    TransactionRepositoryImpl(dataSource) {
    object Tabell {
        override fun toString(): String = "hendelse_eiere"
        val id = "id"
        val ident = "ident"
        val hendelseId = "hendelse_id"
    }

    override fun upsert(connection: Connection, hendelseEier: HendelseEierDAO): HendelseEierDAO? {
        return connection.executeWithResult(
            """
           INSERT INTO ${Tabell}(${Tabell.ident}, ${Tabell.hendelseId}) VALUES(?, ?)
            ON CONFLICT DO NOTHING
            RETURNING *;
        """.trimIndent(), hendelseEier.ident, hendelseEier.hendelseId
        ) {
            HendelseEierDAO(
                id = it.getString(Tabell.id),
                ident = it.getString(Tabell.ident),
                hendelseId = it.getString(Tabell.hendelseId)
            )
        }.firstOrNull()
    }
}
