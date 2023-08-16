package no.nav.modia.soknadsstatus

import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import no.nav.modia.soknadsstatus.SqlDsl.execute
import no.nav.modia.soknadsstatus.SqlDsl.executeQuery
import java.sql.Timestamp
import javax.sql.DataSource

interface SoknadsstatusRepository {
    fun get(ident: Array<String>): Result<List<SoknadsstatusDomain.SoknadsstatusOppdatering>>
    suspend fun upsert(state: SoknadsstatusDomain.SoknadsstatusOppdatering): Result<Boolean>
}

class SoknadsstatusRepositoryImpl(private val dataSource: DataSource) : SoknadsstatusRepository {
    private object Tabell {
        override fun toString(): String = "soknadsstatus"
        const val ident = "ident"
        const val behandlingsId = "behandlingsId"
        const val systemRef = "systemRef"
        const val tema = "tema"
        const val status = "status"
        const val tidspunkt = "tidspunkt"
    }

    override fun get(idents: Array<String>): Result<List<SoknadsstatusDomain.SoknadsstatusOppdatering>> {
        val preparedVariables = idents.map { "?" }.joinToString()


        return dataSource.executeQuery("SELECT * from $Tabell where ${Tabell.ident} IN ($preparedVariables)", *idents) {
            SoknadsstatusDomain.SoknadsstatusOppdatering(
                ident = it.getString(Tabell.ident),
                behandlingsId = it.getString(Tabell.behandlingsId),
                systemRef = it.getString(Tabell.systemRef),
                tema = it.getString(Tabell.tema),
                status = SoknadsstatusDomain.Status.valueOf(it.getString(Tabell.status)),
                tidspunkt = it.getTimestamp(Tabell.tidspunkt).toInstant().toKotlinInstant(),
            )
        }
    }

    override suspend fun upsert(status: SoknadsstatusDomain.SoknadsstatusOppdatering): Result<Boolean> {
        val tidspunkt = Timestamp.from(status.tidspunkt.toJavaInstant())
        return dataSource.execute(
            """
            INSERT INTO $Tabell
            VALUES (?, ?, ?, ?, ?::statusenum, ?)
            ON CONFLICT (${Tabell.ident}, ${Tabell.behandlingsId}) DO
            UPDATE SET ${Tabell.status} = ?::statusenum, ${Tabell.tidspunkt} = ? WHERE $Tabell.${Tabell.tidspunkt} < ?
            """.trimIndent(),
            status.ident,
            status.behandlingsId,
            status.systemRef,
            status.tema,
            status.status.name,
            tidspunkt,
            status.status.name,
            tidspunkt,
            tidspunkt,
        )
    }
}
