package no.nav.modia.soknadsstatus

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
import no.nav.modia.soknadsstatus.soknadsstatusDomain.soknadsstatus
import no.nav.modia.soknadsstatus.soknadsstatusDomain.soknadsstatusOppdatering
import no.nav.modia.soknadsstatus.soknadsstatusDomain.soknadsstatuser
import no.nav.modia.soknadsstatus.pdl.PdlOppslagService
import no.nav.personoversikt.common.ktor.utils.Security
import no.nav.personoversikt.common.logging.Logging.secureLog
import org.slf4j.event.Level

fun Application.soknadsstatusModule(
    configuration: Configuration,
    repository: soknadsstatusRepository,
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
        filter { call -> call.request.path().startsWith("/modia-soknadsstatus/api") }
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
            route("soknadsstatus") {
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

private fun soknadsstatusRepository.hentAggregert(ident: String): Result<soknadsstatuser> {
    return this.get(ident)
        .map { oppdateringer ->
            val temamap = mutableMapOf<String, soknadsstatus>()
            for (oppdatering in oppdateringer) {
                val temastatus = temamap[oppdatering.tema] ?: soknadsstatus()
                when (oppdatering.status) {
                    soknadsstatusDomain.Status.UNDER_BEHANDLING -> temastatus.underBehandling++
                    soknadsstatusDomain.Status.FERDIG_BEHANDLET -> temastatus.ferdigBehandlet++
                    soknadsstatusDomain.Status.AVBRUTT -> temastatus.avbrutt++
                }
                temamap[oppdatering.tema] = temastatus
            }
            soknadsstatuser(ident = ident, tema = temamap)
        }
}

fun deserialize(key: String?, value: String): soknadsstatusDomain.soknadsstatusInnkommendeOppdatering? {
    return try {
        Json.decodeFromString(value)
    } catch (e: Exception) {
        secureLog.error("Failed to decode message", e)
        null
    }
}

fun persist(key: String?, innkommendeOppdatering: soknadsstatusDomain.soknadsstatusInnkommendeOppdatering?, pdlOppslagService: PdlOppslagService, repository: soknadsstatusRepository) {
    if (innkommendeOppdatering != null) {
        runBlocking(Dispatchers.IO) {
            for (aktoerId in innkommendeOppdatering.aktorIder) {
                launch { fetchIdentAndPersist(aktoerId, innkommendeOppdatering, pdlOppslagService, repository) }
            }
        }
    }
}

fun fetchIdentAndPersist(aktoerId: String, innkommendeOppdatering: soknadsstatusDomain.soknadsstatusInnkommendeOppdatering, pdlOppslagService: PdlOppslagService, repository: soknadsstatusRepository) {
    try {
        val ident = pdlOppslagService.hentFnr(aktoerId) ?: throw NotFoundException("Fant ikke ident for aktørId $aktoerId")
        val soknadsstatus = soknadsstatusOppdatering(
            ident = ident,
            behandlingsId = innkommendeOppdatering.behandlingsId,
            systemRef = innkommendeOppdatering.systemRef,
            tema = innkommendeOppdatering.tema,
            status = innkommendeOppdatering.status,
            tidspunkt = innkommendeOppdatering.tidspunkt
        )
        repository.upsert(soknadsstatus)
    } catch (e: Exception) {
        secureLog.error("Failed to store søknadstatus", e)
    }
}
