package no.nav.modia.soknadsstatus.repository

import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import no.nav.modia.soknadsstatus.SoknadsstatusDomain
import no.nav.modia.soknadsstatus.SqlDsl.executeQuery
import no.nav.modia.soknadsstatus.SqlDsl.executeWithResult
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import javax.sql.DataSource
import no.nav.modia.soknadsstatus.repository.HendelseEierRepositoryImpl.Tabell as HendelseEierTabell

interface HendelseRepository : TransactionRepository {
    suspend fun create(
        connection: Connection,
        hendelse: SoknadsstatusDomain.Hendelse,
    ): SoknadsstatusDomain.Hendelse?

    suspend fun getById(id: String): SoknadsstatusDomain.Hendelse?
    suspend fun getByIdents(idents: Array<String>): List<SoknadsstatusDomain.Hendelse>

    suspend fun getForBehandlingId(behandlingId: String): List<SoknadsstatusDomain.Hendelse>
}

class HendelseRepositoryImpl(dataSource: DataSource) : HendelseRepository, TransactionRepositoryImpl(dataSource) {
    object Tabell {
        override fun toString(): String = "hendelser"
        const val id = "id"
        const val modia_behandlingId = "modia_behandling_id"
        const val hendelseId = "hendelses_id"
        const val behandlingId = "behandling_id"
        const val behandlingsTema = "behandlingstema"
        const val behandlingsType = "behandlingstype"
        const val hendelseProdusent = "hendelse_produsent"
        const val hendelseTidspunkt = "hendelse_tidspunkt"
        const val hendelseType = "hendelse_type"
        const val status = "status"
        const val ansvarligEnhet = "ansvarlig_enhet"
    }

    override suspend fun create(
        connection: Connection,
        hendelse: SoknadsstatusDomain.Hendelse,
    ): SoknadsstatusDomain.Hendelse? {
        return connection.executeWithResult(
            """
            INSERT INTO $Tabell(${Tabell.modia_behandlingId}, ${Tabell.behandlingId}, ${Tabell.behandlingsTema}, ${Tabell.behandlingsType}, ${Tabell.hendelseId}, ${Tabell.hendelseProdusent}, ${Tabell.hendelseTidspunkt}, ${Tabell.hendelseType}, ${Tabell.status}, ${Tabell.ansvarligEnhet})
            VALUES (?::uuid, ?, ?, ?, ?, ?, ?, ?::hendelseTypeEnum, ?::statusEnum, ?)
            RETURNING *;
            """.trimIndent(),
            hendelse.modiaBehandlingId,
            hendelse.behandlingId,
            hendelse.behandlingsTema,
            hendelse.behandlingsType,
            hendelse.hendelseId,
            hendelse.hendelseProdusent,
            Timestamp.valueOf(hendelse.hendelseTidspunkt.toJavaLocalDateTime()),
            hendelse.hendelseType.name,
            hendelse.status.name,
            hendelse.ansvarligEnhet,
        ) {
            convertResultSetToHendelseDAO(it)
        }.firstOrNull()
    }

    override suspend fun getById(id: String): SoknadsstatusDomain.Hendelse? {
        return dataSource.executeQuery("SELECT * FROM $Tabell WHERE $Tabell.${Tabell.id} = ?", id) {
            convertResultSetToHendelseDAO(it)
        }.firstOrNull()
    }

    override suspend fun getByIdents(idents: Array<String>): List<SoknadsstatusDomain.Hendelse> {
        val preparedVariables = createPreparedVariables(idents.size)
        return dataSource.executeQuery(
            """
            SELECT DISTINCT ON ($Tabell.${Tabell.id}) *
            FROM $Tabell
            LEFT JOIN $HendelseEierTabell ON $HendelseEierTabell.${HendelseEierTabell.hendelseId} = $Tabell.${Tabell.id}
            WHERE $HendelseEierTabell.${HendelseEierTabell.ident} IN ($preparedVariables)
            """.trimIndent(),
            *idents,
        ) {
            convertResultSetToHendelseDAO(it)
        }
    }

    override suspend fun getForBehandlingId(behandlingId: String): List<SoknadsstatusDomain.Hendelse> {
        return dataSource.executeQuery("SELECT * FROM $Tabell WHERE $Tabell.${Tabell.behandlingId} = ?", behandlingId) {
            convertResultSetToHendelseDAO(it)
        }
    }

    private fun convertResultSetToHendelseDAO(resultSet: ResultSet): SoknadsstatusDomain.Hendelse {
        return SoknadsstatusDomain.Hendelse(
            id = resultSet.getString(Tabell.id),
            modiaBehandlingId = resultSet.getString(Tabell.modia_behandlingId),
            hendelseId = resultSet.getString(Tabell.hendelseId),
            behandlingId = resultSet.getString(Tabell.behandlingId),
            behandlingsTema = resultSet.getString(Tabell.behandlingsTema),
            behandlingsType = resultSet.getString(Tabell.behandlingsType),
            hendelseProdusent = resultSet.getString(Tabell.hendelseProdusent),
            hendelseTidspunkt = resultSet.getTimestamp(Tabell.hendelseTidspunkt).toLocalDateTime().toKotlinLocalDateTime(),
            hendelseType = SoknadsstatusDomain.HendelseType.valueOf(resultSet.getString(Tabell.hendelseType)),
            status = SoknadsstatusDomain.Status.valueOf(resultSet.getString(Tabell.status)),
            ansvarligEnhet = resultSet.getString(Tabell.ansvarligEnhet),
        )
    }
}
