package no.nav.modia.soknadsstatus.repository

import javax.sql.DataSource

interface IdentRepository {
    suspend fun create(ident: String): Result<Boolean>
}

class IdentRepositoryImpl(dataSource: DataSource): IdentRepository {
    companion object {
        override fun toString(): String = "identer"
        const val ident = "ident"
    }

    override suspend fun create(ident: String): Result<Boolean> {
        TODO("Not yet implemented")
    }

}