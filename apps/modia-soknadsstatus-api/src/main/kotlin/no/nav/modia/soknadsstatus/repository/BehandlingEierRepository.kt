package no.nav.modia.soknadsstatus.repository

import kotlinx.serialization.Serializable
import no.nav.modia.soknadsstatus.SqlDsl.executeWithResult
import java.sql.Connection
import javax.sql.DataSource

interface BehandlingEiereRepository : TransactionRepository {
    suspend fun upsert(
        connection: Connection,
        behandlingEier: BehandlingEierDAO,
    ): BehandlingEierDAO?
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
}
