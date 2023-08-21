package no.nav.modia.soknadsstatus.repository

import kotlinx.serialization.Serializable
import javax.sql.DataSource

interface BehandlingEiereRepository {
    fun getForIdent(ident: String): List<BehandlingEier>
}

@Serializable
data class BehandlingEier(
    val ident: String,
    val behandlingId: String,
)

class BehandlingEiereRepositoryImpl(dataSource: DataSource) : BehandlingEiereRepository {
    object Tabell {
        override fun toString(): String = "behandling_eiere"
        const val ident = "ident"
        const val behandlingId = "behandling_id"
    }

    override fun getForIdent(ident: String): List<BehandlingEier> {
        TODO("Not yet implemented")
    }

}