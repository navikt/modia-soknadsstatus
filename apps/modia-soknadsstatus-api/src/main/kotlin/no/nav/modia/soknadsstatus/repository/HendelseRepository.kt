package no.nav.modia.soknadsstatus.repository

import kotlinx.datetime.toKotlinLocalDateTime
import no.nav.modia.soknadsstatus.SoknadsstatusDomain
import no.nav.modia.soknadsstatus.SqlDsl.executeWithResult
import java.sql.Connection
import javax.sql.DataSource

interface HendelseRepository : TransactionRepository {
    suspend fun create(
        connection: Connection,
        hendelse: SoknadsstatusDomain.HendelseDAO
    ): SoknadsstatusDomain.HendelseDAO?
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
    }


    override suspend fun create(
        connection: Connection,
        hendelse: SoknadsstatusDomain.HendelseDAO
    ): SoknadsstatusDomain.HendelseDAO? {
        return connection.executeWithResult(
            """
            INSERT INTO $Tabell(${Tabell.hendelseId}, ${Tabell.behandlingId}, ${Tabell.hendelseProdusent}, ${Tabell.hendelseTidspunkt}, ${Tabell.hendelseType}, ${Tabell.status}, ${Tabell.ansvarligEnhet})
            VALUES (?, ?, ?, ?, ?, ?::statusEnum, ?, ?)
             RETURNING *;
        """.trimIndent(),
            hendelse.hendelseId,
            hendelse.behandlingId,
            hendelse.hendelseProdusent,
            hendelse.hendelseTidspunkt,
            hendelse.hendelseType,
            hendelse.status,
            hendelse.ansvarligEnhet,
        ) {
            SoknadsstatusDomain.HendelseDAO(
                id = it.getString(Tabell.id),
                hendelseId = it.getString(Tabell.hendelseId),
                behandlingId = it.getString(Tabell.behandlingId),
                hendelseProdusent = it.getString(Tabell.hendelseProdusent),
                hendelseTidspunkt = it.getTimestamp(Tabell.hendelseTidspunkt).toLocalDateTime().toKotlinLocalDateTime(),
                hendelseType = SoknadsstatusDomain.HendelseType.valueOf(it.getString(Tabell.hendelseType)),
                status = SoknadsstatusDomain.Status.valueOf(it.getString(Tabell.status)),
                ansvarligEnhet = it.getString(Tabell.ansvarligEnhet),
            )
        }.firstOrNull()
    }
}