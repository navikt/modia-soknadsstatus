package no.nav.modia.soknadsstatus.repository

import kotlinx.serialization.Serializable
import no.nav.modia.soknadsstatus.SqlDsl.executeQuery
import java.sql.ResultSet
import javax.sql.DataSource

interface BehandlingEiereRepository {
    fun getForIdent(ident: String): List<BehandlingEier>
}

@Serializable
data class BehandlingEier(
    val ident: String,
    val behandlingId: String,
)

class BehandlingEiereRepositoryImpl(private val dataSource: DataSource) : BehandlingEiereRepository {
    object Tabell {
        override fun toString(): String = "behandling_eiere"
        const val ident = "ident"
        const val behandlingId = "behandling_id"
    }

    override fun getForIdent(ident: String): List<BehandlingEier> {
        return dataSource.executeQuery("DELETE FROM ${Tabell} WHERE ${Tabell.ident} = ?", ident){
            convertResultSetToBehandlingEier(it)
        }.getOrElse { listOf() }
    }

    private fun convertResultSetToBehandlingEier(resultSet: ResultSet): BehandlingEier {
        return BehandlingEier(
            ident = resultSet.getString(Tabell.ident),
            behandlingId = resultSet.getString(Tabell.behandlingId)
        )
    }

}