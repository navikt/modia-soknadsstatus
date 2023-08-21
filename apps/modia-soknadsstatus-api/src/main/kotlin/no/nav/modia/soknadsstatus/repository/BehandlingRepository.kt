package no.nav.modia.soknadsstatus.repository

import kotlinx.datetime.toKotlinLocalDateTime
import no.nav.modia.soknadsstatus.SoknadsstatusDomain
import no.nav.modia.soknadsstatus.SqlDsl.execute
import no.nav.modia.soknadsstatus.SqlDsl.executeQuery
import java.sql.ResultSet
import javax.sql.DataSource

interface BehandlingRepository {
    suspend fun create(behandling: SoknadsstatusDomain.BehandlingDAO): Result<Boolean>
    suspend fun update(
        id: String,
        behandling: SoknadsstatusDomain.BehandlingDAO
    ): Result<SoknadsstatusDomain.BehandlingDAO>

    suspend fun get(id: String): Result<SoknadsstatusDomain.BehandlingDAO?>
    suspend fun getByIdents(idents: Array<String>): Result<List<SoknadsstatusDomain.BehandlingDAO>>
    suspend fun delete(id: String)
}

class BehandlingRepositoryImpl(private val dataSource: DataSource) : BehandlingRepository {
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


    override suspend fun create(behandling: SoknadsstatusDomain.BehandlingDAO): Result<Boolean> {
        return dataSource.execute(
            """
               INSERT INTO ${Tabell}(${Tabell.behandlingId}, ${Tabell.produsentSystem}, ${Tabell.startTidspunkt}, ${Tabell.sluttTidspunkt}, ${Tabell.sistOppdatert}, ${Tabell.sakstema}, ${Tabell.behandlingstema}, ${Tabell.status}, ${Tabell.ansvarligEnhet}, ${Tabell.primaerBehandlingId})
               VALUES (?, ?, ?, ?, ?, ?, ?, ?::statusEnum, ?, ?)
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
            behandling.primaerBehandling
        )
    }

    override suspend fun update(
        id: String,
        behandling: SoknadsstatusDomain.BehandlingDAO
    ): Result<SoknadsstatusDomain.BehandlingDAO> {
        TODO("Not yet implemented")
    }

    override suspend fun get(id: String): Result<SoknadsstatusDomain.BehandlingDAO?> {
        val result =
            dataSource.executeQuery("SELECT * FROM $Tabell WHERE ${Tabell.id} = ?") {
                convertResultSetToBehandlingDao(it)
            }
        if (result.isSuccess) {
            return Result.success(result.getOrNull()?.firstOrNull())
        } else {
            return Result.failure(result.exceptionOrNull() ?: throw UnknownError("Ukjent feil oppsto"))
        }
    }

    override suspend fun getByIdents(idents: Array<String>): Result<List<SoknadsstatusDomain.BehandlingDAO>> {
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

    override suspend fun delete(id: String) {
        TODO("Not yet implemented")
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