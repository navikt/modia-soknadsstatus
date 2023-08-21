package no.nav.modia.soknadsstatus.repository

import no.nav.modia.soknadsstatus.SoknadsstatusDomain
import no.nav.modia.soknadsstatus.SqlDsl.execute
import javax.sql.DataSource

interface HendelseRepository {
    suspend fun create(hendelse: SoknadsstatusDomain.HendelseDAO): Result<Boolean>
    suspend fun update(hendelseId: String, hendelse: SoknadsstatusDomain.HendelseDAO)
}

class HendelseRepositoryImpl(private val dataSource: DataSource) : HendelseRepository {
    object Tabell {
        override fun toString(): String = "hendelser"
        const val id = "id"
        const val hendelseId = "hendelseId"
        const val behandlingId = "behandlingId"
        const val hendelseProdusent = "hendelse_produsent"
        const val hendelseType = "hendelse_type"
        const val status = "status"
        const val ansvarligEnhet = "ansvarlig_enhet"
        const val produsentSystem = "produsent_system"
    }


    override suspend fun create(hendelse: SoknadsstatusDomain.HendelseDAO): Result<Boolean> {
        return dataSource.execute(
            """
            INSERT INTO $Tabell(${Tabell.hendelseId}, ${Tabell.behandlingId}, ${Tabell.hendelseProdusent}, ${Tabell.hendelseType}, ${Tabell.status}, ${Tabell.ansvarligEnhet}, ${Tabell.produsentSystem})
            VALUES (?, ?, ?, ?, ?::statusEnum, ?, ?) 
        """.trimIndent(),
            hendelse.hendelseId,
            hendelse.hendelseTidspunkt,
            hendelse.hendelseProdusent,
            hendelse.hendelseType,
            hendelse.status,
            hendelse.ansvarligEnhet,
            hendelse.produsentSystem
        )
    }

    override suspend fun update(hendelseId: String, hendelse: SoknadsstatusDomain.HendelseDAO) {
        TODO("Not yet implemented")
    }

}