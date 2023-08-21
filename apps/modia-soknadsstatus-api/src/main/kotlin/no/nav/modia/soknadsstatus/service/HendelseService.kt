package no.nav.modia.soknadsstatus.service

import no.nav.modia.soknadsstatus.SoknadsstatusDomain
import no.nav.modia.soknadsstatus.repository.HendelseRepository

interface HendelseService {
    suspend fun onNewHendelse(hendelse: SoknadsstatusDomain.HendelseDAO): Result<Boolean>
}

class HendelseServiceImpl(hendelseRepository: HendelseRepository, behandlingService: BehandlingService): HendelseService {
    override suspend fun onNewHendelse(hendelse: SoknadsstatusDomain.HendelseDAO): Result<Boolean> {
        TODO("Not yet implemented")
    }

}