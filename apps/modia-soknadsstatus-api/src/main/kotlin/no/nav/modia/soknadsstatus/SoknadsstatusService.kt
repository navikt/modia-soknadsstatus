package no.nav.modia.soknadsstatus

import io.ktor.server.plugins.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.modia.soknadsstatus.pdl.PdlOppslagService
import no.nav.personoversikt.common.logging.TjenestekallLogg

interface SoknadsstatusService {
    suspend fun fetchIdentsAndPersist(innkommendeOppdatering: SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering?)
    fun fetchAggregatedDataForIdent(ident: String): Result<SoknadsstatusDomain.Soknadsstatuser>
    fun fetchDataForIdent(ident: String): Result<List<SoknadsstatusDomain.SoknadsstatusOppdatering>>
}

class SoknadsstatusServiceImpl(
    private val pdlOppslagService: PdlOppslagService,
    private val repository: SoknadsstatusRepository
) : SoknadsstatusService {
    override suspend fun fetchIdentsAndPersist(innkommendeOppdatering: SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering?) {
        coroutineScope {
            innkommendeOppdatering?.aktorIder?.map {
                async { fetchIdentAndPersist(it, innkommendeOppdatering) }
            }?.awaitAll()
        }
    }

    override fun fetchAggregatedDataForIdent(ident: String): Result<SoknadsstatusDomain.Soknadsstatuser> {
        return repository.get(ident)
            .map { oppdateringer ->
                val temamap = mutableMapOf<String, SoknadsstatusDomain.Soknadsstatus>()
                for (oppdatering in oppdateringer) {
                    val temastatus = temamap[oppdatering.tema] ?: SoknadsstatusDomain.Soknadsstatus()
                    when (oppdatering.status) {
                        SoknadsstatusDomain.Status.UNDER_BEHANDLING -> temastatus.underBehandling++
                        SoknadsstatusDomain.Status.FERDIG_BEHANDLET -> temastatus.ferdigBehandlet++
                        SoknadsstatusDomain.Status.AVBRUTT -> temastatus.avbrutt++
                    }
                    temamap[oppdatering.tema] = temastatus
                }
                SoknadsstatusDomain.Soknadsstatuser(ident = ident, tema = temamap)
            }
    }

    override fun fetchDataForIdent(ident: String): Result<List<SoknadsstatusDomain.SoknadsstatusOppdatering>> {
        return repository.get(ident)
    }

    private suspend fun fetchIdentAndPersist(
        aktoerId: String,
        innkommendeOppdatering: SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering
    ) {
        try {
            val ident = pdlOppslagService.hentFnrMedSystemToken(aktoerId)
                ?: throw NotFoundException("Fant ikke ident for aktørId $aktoerId")
            val soknadsstatus = SoknadsstatusDomain.SoknadsstatusOppdatering(
                ident = ident,
                behandlingsId = innkommendeOppdatering.behandlingsId,
                systemRef = innkommendeOppdatering.systemRef,
                tema = innkommendeOppdatering.tema,
                status = innkommendeOppdatering.status,
                tidspunkt = innkommendeOppdatering.tidspunkt
            )
            repository.upsert(soknadsstatus)
        } catch (e: Exception) {
            TjenestekallLogg.error(
                "Failed to store søknadsstatus",
                fields = mapOf("aktoerId" to aktoerId, "oppdatering" to innkommendeOppdatering),
                throwable = e
            )
            throw e
        }
    }
}
