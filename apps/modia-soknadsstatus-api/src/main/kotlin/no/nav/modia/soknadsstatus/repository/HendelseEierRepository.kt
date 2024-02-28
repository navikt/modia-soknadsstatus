package no.nav.modia.soknadsstatus.repository

import kotlinx.serialization.Serializable
import no.nav.modia.soknadsstatus.SqlDsl.executeWithResult
import java.sql.Connection
import javax.sql.DataSource

interface HendelseEierRepository : TransactionRepository {
    suspend fun upsert(
        connection: Connection,
        hendelseEier: HendelseEierDAO,
    ): HendelseEierDAO?
}

@Serializable
data class HendelseEierDAO(
    val id: String? = null,
    val ident: String? = null,
    val aktorId: String? = null,
    val hendelseId: String? = null,
)

class HendelseEierRepositoryImpl(
    dataSource: DataSource,
) : TransactionRepositoryImpl(dataSource),
    HendelseEierRepository {
    object Tabell {
        override fun toString(): String = "hendelse_eiere"

        val id = "id"
        val ident = "ident"
        val aktorId = "aktor_id"
        val hendelseId = "hendelse_id"
    }

    override suspend fun upsert(
        connection: Connection,
        hendelseEier: HendelseEierDAO,
    ): HendelseEierDAO? =
        connection
            .executeWithResult(
                """
                INSERT INTO $Tabell(${Tabell.hendelseId}, ${Tabell.ident}, ${Tabell.aktorId}) VALUES(?::uuid, ?, ?)
                 ON CONFLICT DO NOTHING
                 RETURNING *;
                """.trimIndent(),
                hendelseEier.hendelseId,
                hendelseEier.ident,
                hendelseEier.aktorId,
            ) {
                HendelseEierDAO(
                    id = it.getString(Tabell.id),
                    ident = it.getString(Tabell.ident),
                    hendelseId = it.getString(Tabell.hendelseId),
                )
            }.firstOrNull()
}
