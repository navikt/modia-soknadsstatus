package no.nav.modia.soknadsstatus.repository

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.modia.soknadsstatus.SqlDsl.execute
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
    )
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
    ) {
        connection.execute(
            """
            UPDATE $Tabell
                SET ${Tabell.ident} = Q.ident
                    FROM (select (value->>0) AS aktor_id, (value->>1) AS ident FROM json_array_elements(?)) Q
                WHERE $Tabell.${Tabell.aktorId} = Q.aktor_id AND $Tabell.${Tabell.ident} IS NULL;
            """.trimIndent(),
            Json.encodeToString(mappings),
        )
    }
}
