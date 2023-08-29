package no.nav.modia.soknadsstatus.service

import no.nav.modia.soknadsstatus.repository.BehandlingEier
import no.nav.modia.soknadsstatus.repository.BehandlingEiereRepository
import java.sql.Connection

interface BehandlingEierService {
   suspend fun upsert(connection: Connection? = null, id: String, behandlingEier: BehandlingEier)
}


class BehandlingEierServiceImpl(private val behandlingEiereRepository: BehandlingEiereRepository) : BehandlingEierService {
    override suspend fun upsert(connection: Connection?, id: String, behandlingEier: BehandlingEier) {
        return behandlingEiereRepository.
    }

}