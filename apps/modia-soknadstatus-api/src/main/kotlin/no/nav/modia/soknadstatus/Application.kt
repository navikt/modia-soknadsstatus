package no.nav.modia.soknadstatus

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import no.nav.modia.soknadstatus.SoknadstatusDomain.Soknadstatus
import no.nav.modia.soknadstatus.SoknadstatusDomain.SoknadstatusOppdatering
import no.nav.modia.soknadstatus.SoknadstatusDomain.Soknadstatuser
import no.nav.modia.soknadstatus.pdl.PdlOppslagService
import no.nav.personoversikt.common.ktor.utils.Security
import no.nav.personoversikt.common.logging.Logging.secureLog
import org.slf4j.event.Level

fun Application.soknadstatusModule(
    configuration: Configuration,
    repository: SoknadstatusRepository,
    useMock: Boolean
) {
    val security = Security(
        listOfNotNull(
            configuration.azuread
        )
    )

    install(CORS) {
        anyHost() // TODO kanskje kun whiteliste personoversikt domenene i første omgang?
        allowMethod(HttpMethod.Get)
    }

    install(BaseNaisApp)

    install(Authentication) {
        if (useMock) {
            security.setupMock(this, "Z999999")
        } else {
            security.setupJWT(this)
        }
    }

    install(CallLogging) {
        level = Level.INFO
        disableDefaultColors()
        filter { call -> call.request.path().startsWith("/modia-soknadstatus/api") }
        mdc("userId") { security.getSubject(it).joinToString(";") }
    }

    install(KafkaStreamPlugin) {
        appname = configuration.appname
        brokerUrl = configuration.brokerUrl
        topology {
            stream<String, String>(configuration.sourceTopic)
                .mapValues(::deserialize)
                .foreach { key, value ->
                    persist(key, value, configuration.pdlOppslagService, repository)
                }
        }
    }

    routing {
        route("api") {
            route("soknadstatus") {
                get("oppdateringer/{ident}") {
                    val ident = call.parameters["ident"] ?: throw HttpStatusException(
                        HttpStatusCode.BadRequest,
                        "ident missing in request"
                    )
                    call.respondWithResult(repository.get(ident))
                }

                get("{ident}") {
                    val ident = call.parameters["ident"] ?: throw HttpStatusException(
                        HttpStatusCode.BadRequest,
                        "ident missing in request"
                    )
                    call.respondWithResult(repository.hentAggregert(ident))
                }
            }
        }
    }
}

private fun SoknadstatusRepository.hentAggregert(ident: String): Result<Soknadstatuser> {
    return this.get(ident)
        .map { oppdateringer ->
            val temamap = mutableMapOf<String, Soknadstatus>()
            for (oppdatering in oppdateringer) {
                val temastatus = temamap[oppdatering.tema] ?: Soknadstatus()
                when (oppdatering.status) {
                    SoknadstatusDomain.Status.UNDER_BEHANDLING -> temastatus.underBehandling++
                    SoknadstatusDomain.Status.FERDIG_BEHANDLET -> temastatus.ferdigBehandlet++
                    SoknadstatusDomain.Status.AVBRUTT -> temastatus.avbrutt++
                }
                temamap[oppdatering.tema] = temastatus
            }
            Soknadstatuser(ident = ident, tema = temamap)
        }
}

fun deserialize(key: String?, value: String): SoknadstatusDomain.SoknadstatusInnkommendeOppdatering? {
    return try {
        Json.decodeFromString(value)
    } catch (e: Exception) {
        secureLog.error("Failed to decode message", e)
        null
    }
}

fun persist(key: String?, innkommendeOppdatering: SoknadstatusDomain.SoknadstatusInnkommendeOppdatering?, pdlOppslagService: PdlOppslagService, repository: SoknadstatusRepository) {
    if (innkommendeOppdatering != null) {
        runBlocking(Dispatchers.IO) {
            for (aktoerId in innkommendeOppdatering.aktorIder) {
                launch { fetchIdentAndPersist(aktoerId, innkommendeOppdatering, pdlOppslagService, repository) }
            }
        }
    }
}

fun fetchIdentAndPersist(aktoerId: String, innkommendeOppdatering: SoknadstatusDomain.SoknadstatusInnkommendeOppdatering, pdlOppslagService: PdlOppslagService, repository: SoknadstatusRepository) {
    try {
        val ident = pdlOppslagService.hentFnr(aktoerId) ?: throw NotFoundException("Fant ikke ident for aktørId $aktoerId")
        val soknadstatus = SoknadstatusOppdatering(
            ident = ident,
            behandlingsRef = innkommendeOppdatering.behandlingsRef,
            systemRef = innkommendeOppdatering.systemRef,
            tema = innkommendeOppdatering.tema,
            status = innkommendeOppdatering.status,
            tidspunkt = innkommendeOppdatering.tidspunkt
        )
        repository.upsert(soknadstatus)
    } catch (e: Exception) {
        secureLog.error("Failed to store søknadstatus", e)
    }
}
