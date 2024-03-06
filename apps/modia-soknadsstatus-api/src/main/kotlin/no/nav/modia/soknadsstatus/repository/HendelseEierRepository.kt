package no.nav.modia.soknadsstatus.repository

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.modia.soknadsstatus.SqlDsl.executeUpdate
import no.nav.modia.soknadsstatus.SqlDsl.executeWithResult
import java.sql.Connection
import javax.sql.DataSource

interface HendelseEierRepository : TransactionRepository {
    suspend fun upsert(
        connection: Connection,
        hendelseEier: HendelseEierDAO,
    ): HendelseEierDAO?

    suspend fun updateAktorToFnr(
        connection: Connection,
        mappings: List<Pair<String, String>>,
    ): Int

    suspend fun deleteDuplicateRowsByIdentAktorMapping(
        connection: Connection,
        aktorFnrMapping: List<Pair<String, String>>,
    ): Int
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

    override suspend fun updateAktorToFnr(
        connection: Connection,
        mappings: List<Pair<String, String>>,
    ) = connection.executeUpdate(
        """
        UPDATE $Tabell
            SET ${Tabell.ident} = Q.ident
                FROM (select (value->>0) AS aktor_id, (value->>1) AS ident FROM json_array_elements(?::json)) Q
            WHERE $Tabell.${Tabell.aktorId} = Q.aktor_id AND $Tabell.${Tabell.ident} IS NULL;
        """.trimIndent(),
        Json.encodeToString(mappings),
    )

    override suspend fun deleteDuplicateRowsByIdentAktorMapping(
        connection: Connection,
        aktorFnrMapping: List<Pair<String, String>>,
    ): Int =
        connection.executeUpdate(
            """
            DELETE FROM $Tabell a
            WHERE
                EXISTS (
                    SELECT * from $Tabell b
                    INNER JOIN (
                        SELECT (value->>0) AS aktor_id, (value->>1) AS ident from json_array_elements(?::json)
                    ) AS Q
                    ON Q.ident = b.${Tabell.ident}
                    WHERE
                        b.${Tabell.hendelseId} = a.${Tabell.hendelseId}
                        AND Q.aktor_id = a.${Tabell.aktorId}
                        AND NOT a.${Tabell.id} = b.${Tabell.id}
                )
            """.trimIndent(),
            Json.encodeToString(aktorFnrMapping.map { it.toList() }),
        )
}
