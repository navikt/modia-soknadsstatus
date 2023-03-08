package no.nav.modia.soknadsstatus

import io.ktor.server.plugins.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.modia.soknadsstatus.pdl.PdlOppslagService
import no.nav.personoversikt.common.logging.Logging

interface SoknadsstatusService {
    fun fetchIdentsAndPersist(innkommendeOppdatering: soknadsstatusDomain.soknadsstatusInnkommendeOppdatering?, token: String)
    fun fetchAggregatedDataForIdent(ident: String): Result<soknadsstatusDomain.soknadsstatuser>
    fun fetchDataForIdent(ident: String): Result<List<soknadsstatusDomain.soknadsstatusOppdatering>>
}

class SoknadsstatusServiceImpl(private val pdlOppslagService: PdlOppslagService, private val repository: soknadsstatusRepository) : SoknadsstatusService {
    override fun fetchIdentsAndPersist(innkommendeOppdatering: soknadsstatusDomain.soknadsstatusInnkommendeOppdatering?, token: String) {
        if (innkommendeOppdatering != null) {
            runBlocking(Dispatchers.IO) {
                for (aktoerId in innkommendeOppdatering.aktorIder) {
                    launch { fetchIdentAndPersist(aktoerId, innkommendeOppdatering, token) }
                }
            }
        }
    }

    override fun fetchAggregatedDataForIdent(ident: String): Result<soknadsstatusDomain.soknadsstatuser> {
        return repository.get(ident)
            .map { oppdateringer ->
                val temamap = mutableMapOf<String, soknadsstatusDomain.soknadsstatus>()
                for (oppdatering in oppdateringer) {
                    val temastatus = temamap[oppdatering.tema] ?: soknadsstatusDomain.soknadsstatus()
                    when (oppdatering.status) {
                        soknadsstatusDomain.Status.UNDER_BEHANDLING -> temastatus.underBehandling++
                        soknadsstatusDomain.Status.FERDIG_BEHANDLET -> temastatus.ferdigBehandlet++
                        soknadsstatusDomain.Status.AVBRUTT -> temastatus.avbrutt++
                    }
                    temamap[oppdatering.tema] = temastatus
                }
                soknadsstatusDomain.soknadsstatuser(ident = ident, tema = temamap)
            }
    }

    override fun fetchDataForIdent(ident: String): Result<List<soknadsstatusDomain.soknadsstatusOppdatering>> {
        return repository.get(ident)
    }

    private fun fetchIdentAndPersist(aktoerId: String, innkommendeOppdatering: soknadsstatusDomain.soknadsstatusInnkommendeOppdatering, token: String) {
        try {
            val ident = pdlOppslagService.hentFnr(aktoerId, token) ?: throw NotFoundException("Fant ikke ident for aktørId $aktoerId")
            val soknadsstatus = soknadsstatusDomain.soknadsstatusOppdatering(
                ident = ident,
                behandlingsId = innkommendeOppdatering.behandlingsId,
                systemRef = innkommendeOppdatering.systemRef,
                tema = innkommendeOppdatering.tema,
                status = innkommendeOppdatering.status,
                tidspunkt = innkommendeOppdatering.tidspunkt
            )
            repository.upsert(soknadsstatus)
        } catch (e: Exception) {
            Logging.secureLog.error("Failed to store søknadsstatus", e)
        }
    }
}
