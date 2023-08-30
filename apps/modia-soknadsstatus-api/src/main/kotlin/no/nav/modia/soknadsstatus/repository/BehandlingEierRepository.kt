package no.nav.modia.soknadsstatus.repository

import kotlinx.serialization.Serializable
import no.nav.modia.soknadsstatus.SqlDsl.execute
import no.nav.modia.soknadsstatus.SqlDsl.executeQuery
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

interface BehandlingEiereRepository : TransactionRepository {
    fun create(connection: Connection, eier: BehandlingEierDAO): Boolean

    suspend fun upsert(connection: Connection, eier: BehandlingEierDAO)
    fun getForIdent(ident: String): List<BehandlingEierDAO>
}

@Serializable
data class BehandlingEierDAO(
    val id: String? = null,
    val ident: String? = null,
    val behandlingId: String? = null,
)

class BehandlingEierRepositoryImpl(dataSource: DataSource) : BehandlingEiereRepository,
    TransactionRepositoryImpl(dataSource) {
    object Tabell {
        override fun toString(): String = "behandling_eiere"
        const val ident = "ident"
        const val behandlingId = "behandling_id"
    }

    override fun create(connection: Connection, eier: BehandlingEierDAO): Boolean {
        return connection.execute(
            "INSERT INTO ${Tabell}(${Tabell.ident}, ${Tabell.behandlingId}) VALUES(?, ?)",
            eier.ident,
            eier.behandlingId
        )
    }

    override suspend fun upsert(connection: Connection, eier: BehandlingEierDAO) {
        TODO("Not yet implemented")
    }

    override fun getForIdent(ident: String): List<BehandlingEierDAO> {
        return dataSource.executeQuery("DELETE FROM $Tabell WHERE ${Tabell.ident} = ?", ident) {
            convertResultSetToBehandlingEier(it)
        }
    }

    private fun convertResultSetToBehandlingEier(resultSet: ResultSet): BehandlingEierDAO {
        return BehandlingEierDAO(
            ident = resultSet.getString(Tabell.ident),
            behandlingId = resultSet.getString(Tabell.behandlingId)
        )
    }
}