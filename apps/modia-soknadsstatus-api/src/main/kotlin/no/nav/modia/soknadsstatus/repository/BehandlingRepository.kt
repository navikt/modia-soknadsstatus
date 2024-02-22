package no.nav.modia.soknadsstatus.repository

import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import no.nav.modia.soknadsstatus.SoknadsstatusDomain
import no.nav.modia.soknadsstatus.SqlDsl.execute
import no.nav.modia.soknadsstatus.SqlDsl.executeQuery
import no.nav.modia.soknadsstatus.SqlDsl.executeWithResult
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import javax.sql.DataSource
import no.nav.modia.soknadsstatus.repository.BehandlingEierRepositoryImpl.Tabell as BehandlingEierTabell

interface BehandlingRepository : TransactionRepository {
    suspend fun upsert(
        connection: Connection,
        behandling: SoknadsstatusDomain.Behandling,
    ): SoknadsstatusDomain.Behandling?

    suspend fun getForId(id: String): SoknadsstatusDomain.Behandling?

    suspend fun getByBehandlingId(behandlingId: String): SoknadsstatusDomain.Behandling?

    suspend fun getByIdents(idents: Array<String>): List<SoknadsstatusDomain.Behandling>

    suspend fun delete(id: String)
}

class BehandlingRepositoryImpl(
    dataSource: DataSource,
) : TransactionRepositoryImpl(dataSource),
    BehandlingRepository {
    object Tabell {
        override fun toString(): String = "behandlinger"

        const val id = "id"
        const val behandlingId = "behandling_id"
        const val produsentSystem = "produsent_system"
        const val startTidspunkt = "start_tidspunkt"
        const val sluttTidspunkt = "slutt_tidspunkt"
        const val sistOppdatert = "sist_oppdatert"
        const val sakstema = "sakstema"
        const val behandlingsTema = "behandlingstema"
        const val behandlingsType = "behandlingstype"
        const val status = "status"
        const val ansvarligEnhet = "ansvarlig_enhet"
        const val primaerBehandlingId = "primaer_behandling_id"
        const val primaerBehandlingType = "primaer_behandling_type"
        const val applikasjonSak = "applikasjon_sak"
        const val applikasjonBehandling = "applikasjon_behandling"
        const val sobFlag = "sob_flag"
    }

    override suspend fun upsert(
        connection: Connection,
        behandling: SoknadsstatusDomain.Behandling,
    ): SoknadsstatusDomain.Behandling? =
        connection
            .executeWithResult(
                """
                INSERT INTO $Tabell(${Tabell.behandlingId}, ${Tabell.produsentSystem}, ${Tabell.startTidspunkt}, ${Tabell.sluttTidspunkt}, ${Tabell.sistOppdatert}, ${Tabell.sakstema}, ${Tabell.behandlingsTema}, ${Tabell.behandlingsType}, ${Tabell.status}, ${Tabell.ansvarligEnhet}, ${Tabell.primaerBehandlingId}, ${Tabell.primaerBehandlingType}, ${Tabell.applikasjonSak}, ${Tabell.applikasjonBehandling}, ${Tabell.sobFlag})
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::statusEnum, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (${Tabell.behandlingId}) DO UPDATE SET ${Tabell.status} = ?::statusEnum, ${Tabell.sluttTidspunkt} = ?, ${Tabell.sistOppdatert} = ? WHERE $Tabell.${Tabell.sistOppdatert} <= ?
                RETURNING *;
                """.trimIndent(),
                behandling.behandlingId,
                behandling.produsentSystem,
                Timestamp.valueOf(behandling.startTidspunkt?.toJavaLocalDateTime()),
                Timestamp.valueOf(behandling.sluttTidspunkt?.toJavaLocalDateTime()),
                Timestamp.valueOf(behandling.sistOppdatert.toJavaLocalDateTime()),
                behandling.sakstema,
                behandling.behandlingsTema,
                behandling.behandlingsType,
                behandling.status.name,
                behandling.ansvarligEnhet,
                behandling.primaerBehandlingId,
                behandling.primaerBehandlingType,
                behandling.applikasjonSak,
                behandling.applikasjonBehandling,
                behandling.status.name,
                Timestamp.valueOf(behandling.sluttTidspunkt?.toJavaLocalDateTime()),
                Timestamp.valueOf(behandling.sistOppdatert.toJavaLocalDateTime()),
                Timestamp.valueOf(behandling.sistOppdatert.toJavaLocalDateTime()),
            ) {
                convertResultSetToBehandlingDao(it)
            }.firstOrNull()

    override suspend fun getForId(id: String): SoknadsstatusDomain.Behandling? =
        dataSource
            .executeQuery("SELECT * FROM $Tabell WHERE $Tabell.${Tabell.id} = ?", id) {
                convertResultSetToBehandlingDao(it)
            }.firstOrNull()

    override suspend fun getByBehandlingId(behandlingId: String): SoknadsstatusDomain.Behandling? =
        dataSource
            .executeQuery("SELECT * FROM $Tabell WHERE $Tabell.${Tabell.behandlingId} = ?", behandlingId) {
                convertResultSetToBehandlingDao(it)
            }.firstOrNull()

    override suspend fun getByIdents(idents: Array<String>): List<SoknadsstatusDomain.Behandling> {
        val preparedVariables = createPreparedVariables(idents.size)
        return dataSource.executeQuery(
            """
            SELECT DISTINCT ON ($Tabell.${Tabell.id}) *
            FROM $Tabell
            LEFT JOIN $BehandlingEierTabell ON $BehandlingEierTabell.${BehandlingEierTabell.behandlingId} = $Tabell.${Tabell.id}
            WHERE ${BehandlingEierTabell.ident} IN ($preparedVariables)
            """.trimIndent(),
            *idents,
        ) {
            convertResultSetToBehandlingDao(it)
        }
    }

    override suspend fun delete(id: String) {
        dataSource.execute("DELETE FROM $Tabell WHERE ${Tabell.id} = $id")
    }

    private fun convertResultSetToBehandlingDao(resultSet: ResultSet): SoknadsstatusDomain.Behandling =
        SoknadsstatusDomain.Behandling(
            id = resultSet.getString(Tabell.id),
            behandlingId = resultSet.getString(Tabell.behandlingId),
            produsentSystem = resultSet.getString(Tabell.produsentSystem),
            startTidspunkt = resultSet.getTimestamp(Tabell.startTidspunkt).toLocalDateTime().toKotlinLocalDateTime(),
            sluttTidspunkt = resultSet.getTimestamp(Tabell.sluttTidspunkt).toLocalDateTime().toKotlinLocalDateTime(),
            sistOppdatert = resultSet.getTimestamp(Tabell.sistOppdatert).toLocalDateTime().toKotlinLocalDateTime(),
            sakstema = resultSet.getString(Tabell.sakstema),
            behandlingsTema = resultSet.getString(Tabell.behandlingsTema),
            behandlingsType = resultSet.getString(Tabell.behandlingsType),
            status = SoknadsstatusDomain.Status.valueOf(resultSet.getString(Tabell.status)),
            ansvarligEnhet = resultSet.getString(Tabell.ansvarligEnhet),
            primaerBehandlingId = resultSet.getString(Tabell.primaerBehandlingId),
            primaerBehandlingType = resultSet.getString(Tabell.primaerBehandlingType),
            applikasjonSak = resultSet.getString(Tabell.applikasjonSak),
            applikasjonBehandling = resultSet.getString(Tabell.applikasjonBehandling),
        )
}
