package no.nav.modia.soknadstatus

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import no.nav.modia.soknadstatus.SoknadstatusDomain.Soknadstatus
import no.nav.modia.soknadstatus.SoknadstatusDomain.SoknadstatusOppdatering
import no.nav.modia.soknadstatus.SoknadstatusDomain.Soknadstatuser
import no.nav.personoversikt.common.logging.Logging.secureLog

fun Application.soknadstatusModule() {
    val config = Configuration()

    install(BaseNaisApp)
    install(KafkaStreamPlugin) {
        appname = config.appname
        brokerUrl = config.brokerUrl
        topology {
            stream<String, String>(config.sourceTopic)
                .mapValues(::deserialize)
                .foreach(::persist)
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

fun deserialize(key: String?, value: String): SoknadstatusOppdatering? {
    return try {
        Json.decodeFromString(value)
    } catch (e: Exception) {
        secureLog.error("Failed to decode message", e)
        null
    }
}

fun persist(key: String?, soknadstatusOppdatering: SoknadstatusOppdatering?) {
    if (soknadstatusOppdatering != null) {
        repository.upsert(soknadstatusOppdatering)
    }
}
