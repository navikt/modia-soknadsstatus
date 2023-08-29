package no.nav.modia.soknadsstatus.repository

import kotlinx.serialization.Serializable
import no.nav.modia.soknadsstatus.SqlDsl.execute
import no.nav.modia.soknadsstatus.SqlDsl.executeQuery
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

interface BehandlingEiereRepository : TransactionRepository {
    fun create(connection: Connection, eier: BehandlingEier): Boolean

    suspend fun upsert(connection: Connection, eier: BehandlingEier)
    fun getForIdent(ident: String): List<BehandlingEier>
}

@Serializable
data class BehandlingEier(
    val ident: String,
    val behandlingId: String,
)

class BehandlingEiereRepositoryImpl(dataSource: DataSource) : BehandlingEiereRepository,
    TransactionRepositoryImpl(dataSource) {
    object Tabell {
        override fun toString(): String = "behandling_eiere"
        const val ident = "ident"
        const val behandlingId = "behandling_id"
    }

    override fun create(connection: Connection, eier: BehandlingEier): Boolean {
        return connection.execute(
            "INSERT INTO ${Tabell}(${Tabell.ident}, ${Tabell.behandlingId}) VALUES(?, ?)",
            eier.ident,
            eier.behandlingId
        )
    }

    override fun getForIdent(ident: String): List<BehandlingEier> {
        return dataSource.executeQuery("DELETE FROM $Tabell WHERE ${Tabell.ident} = ?", ident) {
            convertResultSetToBehandlingEier(it)
        }
    }

    private fun convertResultSetToBehandlingEier(resultSet: ResultSet): BehandlingEier {
        return BehandlingEier(
            ident = resultSet.getString(Tabell.ident),
            behandlingId = resultSet.getString(Tabell.behandlingId)
        )
    }
}