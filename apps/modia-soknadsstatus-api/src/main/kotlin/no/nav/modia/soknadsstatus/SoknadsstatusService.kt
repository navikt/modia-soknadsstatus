package no.nav.modia.soknadsstatus

import io.ktor.server.plugins.*
import kotlinx.coroutines.*
import no.nav.api.generated.pdl.enums.IdentGruppe
import no.nav.modia.soknadsstatus.pdl.PdlOppslagService
import no.nav.personoversikt.common.logging.TjenestekallLogg

interface SoknadsstatusService {
    fun fetchAggregatedDataForIdent(userToken: String, ident: String): Result<SoknadsstatusDomain.Soknadsstatuser>
    fun fetchDataForIdent(userToken: String, ident: String): Result<List<SoknadsstatusDomain.SoknadsstatusOppdatering>>
    suspend fun persistUpdate(update: SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering?)
}

class SoknadsstatusServiceImpl(
    private val pdlOppslagService: PdlOppslagService,
    private val repository: SoknadsstatusRepository,
) : SoknadsstatusService {
    override fun fetchAggregatedDataForIdent(
        userToken: String,
        ident: String
    ): Result<SoknadsstatusDomain.Soknadsstatuser> {
        val idents = runBlocking { pdlOppslagService.hentAktiveIdenter(userToken, ident) }
        return repository.get(idents.toTypedArray())
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
                SoknadsstatusDomain.Soknadsstatuser(identer = idents, tema = temamap)
            }
    }

    override fun fetchDataForIdent(
        userToken: String,
        ident: String
    ): Result<List<SoknadsstatusDomain.SoknadsstatusOppdatering>> {
        val idents = runBlocking { pdlOppslagService.hentAktiveIdenter(userToken, ident) }

        return repository.get(idents.toTypedArray())
    }

    override suspend fun persistUpdate(update: SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering?) {
        if (update == null) return
        coroutineScope {
            if (update.aktorIder != null) {
                update.aktorIder?.map {
                    async(Dispatchers.IO) { persistUpdateForAktorId(it, update) }
                }?.awaitAll()
            } else {
                update.identer?.map {
                    async(Dispatchers.IO) { persistUpdateForIdent(it, update) }
                }?.awaitAll()
            }
        }
    }

    private suspend fun persistUpdateForAktorId(
        aktoerId: String,
        update: SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering
    ) {
        TjenestekallLogg.info(
            "Mottok søknadsstatus-oppdatering for aktør",
            mapOf("aktoerId" to aktoerId, "type" to IdentGruppe.AKTORID, "oppdatering" to update)
        )
        try {
            val ident = getIdent(aktoerId)
            val soknadsstatus = SoknadsstatusDomain.SoknadsstatusOppdatering(
                ident = ident,
                behandlingsId = update.behandlingsId,
                systemRef = update.systemRef,
                tema = update.tema,
                status = update.status,
                tidspunkt = update.tidspunkt,
            )
            repository.upsert(soknadsstatus)
        } catch (e: Exception) {
            TjenestekallLogg.error(
                "Failed to store søknadsstatus",
                fields = mapOf("aktoerId" to aktoerId, "type" to IdentGruppe.AKTORID, "oppdatering" to update),
                throwable = e,
            )
            throw e
        }
    }

    private suspend fun persistUpdateForIdent(
        identType: SoknadsstatusDomain.IdentType,
        update: SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering,
    ) {
        TjenestekallLogg.info(
            "Mottok søknadsstatus-oppdatering for ident",
            mapOf("ident" to identType.ident, "type" to identType.type, "oppdatering" to update)
        )
        try {
            val ident = getIdent(identType)
            val soknadsstatus = SoknadsstatusDomain.SoknadsstatusOppdatering(
                ident = ident,
                behandlingsId = update.behandlingsId,
                systemRef = update.systemRef,
                tema = update.tema,
                status = update.status,
                tidspunkt = update.tidspunkt,
            )
            repository.upsert(soknadsstatus)
        } catch (e: Exception) {
            TjenestekallLogg.error(
                "Failed to store søknadsstatus",
                fields = mapOf("ident" to identType.ident, "type" to identType.type, "oppdatering" to update),
                throwable = e,
            )
            throw e
        }
    }

    private suspend fun getIdent(identType: SoknadsstatusDomain.IdentType): String {
        return when (identType.type) {
            IdentGruppe.AKTORID -> {
                pdlOppslagService.hentFnrMedSystemToken(identType.ident)
                    ?: throw NotFoundException("Fant ikke ident for aktørId ${identType.ident}")
            }

            IdentGruppe.FOLKEREGISTERIDENT -> identType.ident
            else -> throw IllegalArgumentException("Mottok ukjent identtype: ${identType.type}")
        }
    }

    private suspend fun getIdent(aktoerId: String): String {
        if (isAktoerId(aktoerId)) return getIdent(
            SoknadsstatusDomain.IdentType(
                ident = aktoerId,
                type = IdentGruppe.AKTORID
            )
        )
        return aktoerId
    }

    private fun isAktoerId(aktoerId: String): Boolean {
        val regex = Regex("^\\d{13}$")

        return aktoerId.matches(regex)

    }
}
