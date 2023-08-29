package no.nav.modia.soknadsstatus.repository

import kotlinx.datetime.toKotlinLocalDateTime
import no.nav.modia.soknadsstatus.SoknadsstatusDomain
import no.nav.modia.soknadsstatus.SoknadsstatusRepositoryImpl
import no.nav.modia.soknadsstatus.SqlDsl.execute
import no.nav.modia.soknadsstatus.SqlDsl.executeQuery
import no.nav.modia.soknadsstatus.SqlDsl.executeWithResult
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

interface BehandlingRepository : TransactionRepository {
    suspend fun upsert(
        connection: Connection,
        behandling: SoknadsstatusDomain.BehandlingDAO
    ): SoknadsstatusDomain.BehandlingDAO?

    suspend fun get(id: String): SoknadsstatusDomain.BehandlingDAO?
    suspend fun getByBehandlingId(connection: Connection, behandlingId: String): SoknadsstatusDomain.BehandlingDAO?
    suspend fun getByIdents(idents: Array<String>): List<SoknadsstatusDomain.BehandlingDAO>
    suspend fun delete(connection: Connection, id: String)
}

class BehandlingRepositoryImpl(dataSource: DataSource) : BehandlingRepository, TransactionRepositoryImpl(dataSource) {
    object Tabell {
        override fun toString(): String = "behandlinger"
        const val id = "id"
        const val behandlingId = "behandling_id"
        const val produsentSystem = "produsent_system"
        const val startTidspunkt = "start_tidspunkt"
        const val sluttTidspunkt = "slutt_tidspunkt"
        const val sistOppdatert = "sist_oppdatert"
        const val sakstema = "sakstema"
        const val behandlingstema = "behandlingstema"
        const val status = "status"
        const val ansvarligEnhet = "ansvarlig_enhet"
        const val primaerBehandlingId = "primaer_behandling_id"
    }

    override suspend fun upsert(
        connection: Connection,
        behandling: SoknadsstatusDomain.BehandlingDAO
    ): SoknadsstatusDomain.BehandlingDAO? {
        return connection.executeWithResult(
            """
               INSERT INTO ${Tabell}(${Tabell.behandlingId}, ${Tabell.produsentSystem}, ${Tabell.startTidspunkt}, ${Tabell.sluttTidspunkt}, ${Tabell.sistOppdatert}, ${Tabell.sakstema}, ${Tabell.behandlingstema}, ${Tabell.status}, ${Tabell.ansvarligEnhet}, ${Tabell.primaerBehandlingId})
               VALUES (?, ?, ?, ?, ?, ?, ?, ?::statusEnum, ?, ?)
               ON CONFLICT ${Tabell.behandlingId} DO
                    UPDATE SET ${Tabell.status} = ?::statusenum, ${Tabell.sistOppdatert} = ? WHERE ${Tabell}.${Tabell.sistOppdatert} < ?
               RETURNING *;
           """.trimIndent(),
            behandling.behandlingId,
            behandling.produsentSystem,
            behandling.startTidspunkt,
            behandling.sluttTidspunkt,
            behandling.sistOppdatert,
            behandling.sakstema,
            behandling.behandlingsTema,
            behandling.status,
            behandling.ansvarligEnhet,
            behandling.primaerBehandling,
            behandling.status,
            behandling.sistOppdatert,
            behandling.sistOppdatert
        ) {
            convertResultSetToBehandlingDao(it)
        }.firstOrNull()
    }

    override suspend fun get(id: String): SoknadsstatusDomain.BehandlingDAO? {
        return dataSource.executeQuery("SELECT * FROM $Tabell WHERE ${Tabell.id} = ?", id) {
            convertResultSetToBehandlingDao(it)
        }.firstOrNull()
    }

    override suspend fun getByBehandlingId(
        connection: Connection,
        behandlingId: String
    ): SoknadsstatusDomain.BehandlingDAO? {
        return connection.executeQuery("SELECT * FROM $Tabell WHERE ${Tabell.behandlingId} = ?", behandlingId) {
            convertResultSetToBehandlingDao(it)
        }.firstOrNull()
    }

    override suspend fun getByIdents(idents: Array<String>): List<SoknadsstatusDomain.BehandlingDAO> {
        val preparedVariables = idents.map { "?" }.joinToString()
        return dataSource.executeQuery(
            """
            SELECT *
            FROM ${BehandlingEiereRepositoryImpl.Tabell}
            LEFT JOIN $Tabell ON ${Tabell.behandlingId} = ${BehandlingEiereRepositoryImpl.Tabell}.${BehandlingEiereRepositoryImpl.Tabell.behandlingId}
            WHERE ${BehandlingEiereRepositoryImpl.Tabell.ident} IN $preparedVariables 
        """.trimIndent(),
            idents
        ) {
            convertResultSetToBehandlingDao(it)
        }
    }

    override suspend fun delete(connection: Connection, id: String) {
        connection.execute("DELETE FROM $Tabell WHERE ${Tabell.id} = $id")
    }

    private fun convertResultSetToBehandlingDao(resultSet: ResultSet): SoknadsstatusDomain.BehandlingDAO {
        return SoknadsstatusDomain.BehandlingDAO(
            id = resultSet.getString(Tabell.id),
            behandlingId = resultSet.getString(Tabell.behandlingId),
            produsentSystem = resultSet.getString(Tabell.produsentSystem),
            startTidspunkt = resultSet.getTimestamp(Tabell.startTidspunkt).toLocalDateTime().toKotlinLocalDateTime(),
            sluttTidspunkt = resultSet.getTimestamp(Tabell.sluttTidspunkt).toLocalDateTime().toKotlinLocalDateTime(),
            sistOppdatert = resultSet.getTimestamp(Tabell.sistOppdatert).toLocalDateTime().toKotlinLocalDateTime(),
            sakstema = resultSet.getString(Tabell.sakstema),
            behandlingsTema = resultSet.getString(Tabell.behandlingstema),
            status = SoknadsstatusDomain.Status.valueOf(resultSet.getString(Tabell.status)),
            ansvarligEnhet = resultSet.getString(Tabell.ansvarligEnhet),
            primaerBehandling = resultSet.getString(Tabell.primaerBehandlingId)
        )
    }
}