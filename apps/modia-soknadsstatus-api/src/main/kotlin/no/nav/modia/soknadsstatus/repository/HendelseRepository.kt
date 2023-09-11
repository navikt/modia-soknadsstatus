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
        hendelse: SoknadsstatusDomain.HendelseDAO,
    ): SoknadsstatusDomain.HendelseDAO?

    suspend fun getById(id: String): SoknadsstatusDomain.HendelseDAO?
    suspend fun getByIdents(idents: List<String>): List<SoknadsstatusDomain.HendelseDAO>

    suspend fun getForBehandlingId(behandlingId: String): List<SoknadsstatusDomain.HendelseDAO>
}

class HendelseRepositoryImpl(dataSource: DataSource) : HendelseRepository, TransactionRepositoryImpl(dataSource) {
    object Tabell {
        override fun toString(): String = "hendelser"
        const val id = "id"
        const val hendelseId = "hendelses_id"
        const val behandlingId = "behandling_id"
        const val hendelseProdusent = "hendelse_produsent"
        const val hendelseTidspunkt = "hendelse_tidspunkt"
        const val hendelseType = "hendelse_type"
        const val status = "status"
        const val ansvarligEnhet = "ansvarlig_enhet"
    }

    override suspend fun create(
        connection: Connection,
        hendelse: SoknadsstatusDomain.HendelseDAO,
    ): SoknadsstatusDomain.HendelseDAO? {
        return connection.executeWithResult(
            """
            INSERT INTO $Tabell(${Tabell.behandlingId}, ${Tabell.hendelseId}, ${Tabell.hendelseProdusent}, ${Tabell.hendelseTidspunkt}, ${Tabell.hendelseType}, ${Tabell.status}, ${Tabell.ansvarligEnhet})
            VALUES (?::uuid, ?, ?, ?, ?::hendelseTypeEnum, ?::statusEnum, ?)
            RETURNING *;
            """.trimIndent(),
            hendelse.behandlingId,
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

    override suspend fun getById(id: String): SoknadsstatusDomain.HendelseDAO? {
        return dataSource.executeQuery("SELECT * FROM $Tabell WHERE $Tabell.${Tabell.id} = ?", id) {
            convertResultSetToHendelseDAO(it)
        }.firstOrNull()
    }

    override suspend fun getByIdents(idents: List<String>): List<SoknadsstatusDomain.HendelseDAO> {
        val preparedVariables = idents.map { "'$it'" }.joinToString(",")
        return dataSource.executeQuery(
            """
            SELECT DISTINCT ON ($Tabell.${Tabell.id}) *
            FROM $Tabell
            LEFT JOIN $HendelseEierTabell ON $HendelseEierTabell.${HendelseEierTabell.hendelseId} = $Tabell.${Tabell.id}
            WHERE $HendelseEierTabell.${HendelseEierTabell.ident} IN ($preparedVariables)
            """.trimIndent(),
        ) {
            convertResultSetToHendelseDAO(it)
        }
    }

    override suspend fun getForBehandlingId(behandlingId: String): List<SoknadsstatusDomain.HendelseDAO> {
        return dataSource.executeQuery("SELECT * FROM $Tabell WHERE $Tabell.${Tabell.behandlingId} = ?", behandlingId) {
            convertResultSetToHendelseDAO(it)
        }
    }

    private fun convertResultSetToHendelseDAO(resultSet: ResultSet): SoknadsstatusDomain.HendelseDAO {
        return SoknadsstatusDomain.HendelseDAO(
            id = resultSet.getString(Tabell.id),
            hendelseId = resultSet.getString(Tabell.hendelseId),
            behandlingId = resultSet.getString(Tabell.behandlingId),
            hendelseProdusent = resultSet.getString(Tabell.hendelseProdusent),
            hendelseTidspunkt = resultSet.getTimestamp(Tabell.hendelseTidspunkt).toLocalDateTime().toKotlinLocalDateTime(),
            hendelseType = SoknadsstatusDomain.HendelseType.valueOf(resultSet.getString(Tabell.hendelseType)),
            status = SoknadsstatusDomain.Status.valueOf(resultSet.getString(Tabell.status)),
            ansvarligEnhet = resultSet.getString(Tabell.ansvarligEnhet),
        )
    }
}
