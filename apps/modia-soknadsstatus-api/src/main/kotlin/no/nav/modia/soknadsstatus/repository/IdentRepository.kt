package no.nav.modia.soknadsstatus.repository

import no.nav.modia.soknadsstatus.SqlDsl.execute
import javax.sql.DataSource

interface IdentRepository {
    suspend fun create(ident: String): Result<Boolean>
}

class IdentRepositoryImpl(private val dataSource: DataSource): IdentRepository {
    object Tabell {
        override fun toString(): String = "identer"
        const val ident = "ident"
    }

    override suspend fun create(ident: String): Result<Boolean> {
        return dataSource.execute(
            """
               INSERT INTO ${Tabell}(${Tabell.ident})
               VALUES (?)
           """.trimIndent(),
            ident
        )
    }

}