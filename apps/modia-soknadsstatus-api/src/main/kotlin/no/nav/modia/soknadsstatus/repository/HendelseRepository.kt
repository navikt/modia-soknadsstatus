package no.nav.modia.soknadsstatus.repository

import no.nav.modia.soknadsstatus.SoknadsstatusDomain
import no.nav.modia.soknadsstatus.SqlDsl.execute
import java.sql.Connection
import javax.sql.DataSource

interface HendelseRepository : TransactionRepository {
    suspend fun create(connection: Connection, hendelse: SoknadsstatusDomain.HendelseDAO): Boolean
    suspend fun update(
        connection: Connection,
        hendelseId: String,
        hendelse: SoknadsstatusDomain.HendelseDAO
    ): Boolean
}

class HendelseRepositoryImpl(dataSource: DataSource) : HendelseRepository, TransactionRepositoryImpl(dataSource) {
    object Tabell {
        override fun toString(): String = "hendelser"
        const val id = "id"
        const val hendelseId = "hendelseId"
        const val behandlingId = "behandlingId"
        const val hendelseProdusent = "hendelse_produsent"
        const val hendelseTidspunkt = "hendelse_tidspunkt"
        const val hendelseType = "hendelse_type"
        const val status = "status"
        const val ansvarligEnhet = "ansvarlig_enhet"
        const val produsentSystem = "produsent_system"
    }


    override suspend fun create(connection: Connection, hendelse: SoknadsstatusDomain.HendelseDAO): Boolean {
        return connection.execute(
            """
            INSERT INTO $Tabell(${Tabell.hendelseId}, ${Tabell.behandlingId}, ${Tabell.hendelseProdusent}, ${Tabell.hendelseTidspunkt}, ${Tabell.hendelseType}, ${Tabell.status}, ${Tabell.ansvarligEnhet}, ${Tabell.produsentSystem})
            VALUES (?, ?, ?, ?, ?, ?::statusEnum, ?, ?) 
        """.trimIndent(),
            hendelse.hendelseId,
            hendelse.behandlingId,
            hendelse.hendelseProdusent,
            hendelse.hendelseTidspunkt,
            hendelse.hendelseType,
            hendelse.status,
            hendelse.ansvarligEnhet,
            hendelse.produsentSystem
        )
    }

    override suspend fun update(
        connection: Connection,
        hendelseId: String,
        hendelse: SoknadsstatusDomain.HendelseDAO
    ): Boolean {
        return connection.execute(
            """
            UPDATE $Tabell SET ${Tabell.hendelseProdusent} = ?, ${Tabell.hendelseTidspunkt} = ?, ${Tabell.hendelseType} = ?, ${Tabell.status} = ?, ${Tabell.ansvarligEnhet} = ?, ${Tabell.produsentSystem} = ?  WHERE ${Tabell.hendelseId} = ? 
        """.trimIndent(),
            hendelse.hendelseProdusent,
            hendelse.hendelseTidspunkt,
            hendelse.hendelseType,
            hendelse.status,
            hendelse.ansvarligEnhet,
            hendelse.produsentSystem,
            hendelseId
        )
    }

}