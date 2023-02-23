package no.nav.modia.soknadsstatus

import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import no.nav.modia.soknadsstatus.SqlDsl.execute
import no.nav.modia.soknadsstatus.SqlDsl.executeQuery
import java.sql.Timestamp
import javax.sql.DataSource

class soknadsstatusRepository(private val dataSource: DataSource) {
    private object Tabell {
        override fun toString(): String = "soknadsstatus"
        val ident = "ident"
        val behandlingsRef = "behandlingsRef"
        val systemRef = "systemRef"
        val tema = "tema"
        val status = "status"
        val tidspunkt = "tidspunkt"
    }

    fun get(ident: String): Result<List<soknadsstatusDomain.soknadsstatusOppdatering>> {
        return dataSource.executeQuery("SELECT * from $Tabell where ${Tabell.ident} = ?", ident).map { rows ->
            rows.map {
                soknadsstatusDomain.soknadsstatusOppdatering(
                    ident = it.rs.getString(Tabell.ident),
                    behandlingsRef = it.rs.getString(Tabell.behandlingsRef),
                    systemRef = it.rs.getString(Tabell.systemRef),
                    tema = it.rs.getString(Tabell.tema),
                    status = soknadsstatusDomain.Status.valueOf(it.rs.getString(Tabell.status)),
                    tidspunkt = it.rs.getTimestamp(Tabell.tidspunkt).toInstant().toKotlinInstant()
                )
            }.toList()
        }
    }

    fun upsert(status: soknadsstatusDomain.soknadsstatusOppdatering): Result<Boolean> {
        val tidspunkt = Timestamp.from(status.tidspunkt.toJavaInstant())
        return dataSource.execute(
            """
            INSERT INTO $Tabell
            VALUES (?, ?, ?, ?, ?::statusenum, ?)
            ON CONFLICT (${Tabell.ident}, ${Tabell.behandlingsRef}) DO
            UPDATE SET ${Tabell.status} = ?::statusenum, ${Tabell.tidspunkt} = ? WHERE $Tabell.${Tabell.tidspunkt} < ?
            """.trimIndent(),
            status.ident,
            status.behandlingsRef,
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
