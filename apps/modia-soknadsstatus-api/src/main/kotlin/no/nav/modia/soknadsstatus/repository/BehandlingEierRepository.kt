package no.nav.modia.soknadsstatus.repository

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.modia.soknadsstatus.SqlDsl.executeUpdate
import no.nav.modia.soknadsstatus.SqlDsl.executeWithResult
import java.sql.Connection
import javax.sql.DataSource

interface BehandlingEiereRepository : TransactionRepository {
    suspend fun upsert(
        connection: Connection,
        behandlingEier: BehandlingEierDAO,
    ): BehandlingEierDAO?

    suspend fun getAktorIdsToConvert(
        connection: Connection,
        limit: Int = 1000,
    ): List<String>

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
data class BehandlingEierDAO(
    val id: String? = null,
    val ident: String? = null,
    val aktorId: String? = null,
    val behandlingId: String? = null,
)

class BehandlingEierRepositoryImpl(
    dataSource: DataSource,
) : TransactionRepositoryImpl(dataSource),
    BehandlingEiereRepository {
    object Tabell {
        override fun toString(): String = "behandling_eiere"

        val id = "id"
        const val ident = "ident"
        const val aktorId = "aktor_id"
        const val behandlingId = "behandling_id"
    }

    override suspend fun upsert(
        connection: Connection,
        behandlingEier: BehandlingEierDAO,
    ): BehandlingEierDAO? =
        connection
            .executeWithResult(
                """
                INSERT INTO $Tabell(${Tabell.behandlingId}, ${Tabell.ident}, ${Tabell.aktorId}) VALUES(?::uuid, ?, ?)
                 ON CONFLICT DO NOTHING
                 RETURNING *;
                """.trimIndent(),
                behandlingEier.behandlingId,
                behandlingEier.ident,
                behandlingEier.aktorId,
            ) {
                BehandlingEierDAO(
                    id = it.getString(Tabell.id),
                    ident = it.getString(Tabell.ident),
                    behandlingId = it.getString(Tabell.behandlingId),
                )
            }.firstOrNull()

    override suspend fun getAktorIdsToConvert(
        connection: Connection,
        limit: Int,
    ): List<String> =
        connection.executeWithResult(
            """
            SELECT ${Tabell.aktorId} FROM $Tabell WHERE ${Tabell.aktorId} IS NOT NULL AND ${Tabell.ident} IS NULL GROUP BY ${Tabell.aktorId} LIMIT ?
            """.trimIndent(),
            limit,
        ) {
            it.getString(Tabell.aktorId)
        }

    override suspend fun updateAktorToFnr(
        connection: Connection,
        mappings: List<Pair<String, String>>,
    ): Int =
        connection.executeUpdate(
            """
            UPDATE $Tabell
                SET ${Tabell.ident} = Q.ident
                    FROM (select (value->>0) AS aktor_id, (value->>1) AS ident FROM json_array_elements(?::json)) Q
                WHERE $Tabell.${Tabell.aktorId} = Q.aktor_id AND $Tabell.${Tabell.ident} IS NULL;
            """.trimIndent(),
            Json.encodeToString(mappings.map { it.toList() }),
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
                        SELECT (value->>0) AS aktor_id, (value->>1) AS ident from json_array_element(?::json)
                    ) AS Q
                    ON Q.ident = b.${Tabell.ident}
                    WHERE
                        b.${Tabell.behandlingId} = a.${Tabell.behandlingId}
                        AND Q.aktor_id = a.${Tabell.aktorId}
                        AND NOT a.${Tabell.id} = b.${Tabell.id}
                )
            """.trimIndent(),
        )
}
