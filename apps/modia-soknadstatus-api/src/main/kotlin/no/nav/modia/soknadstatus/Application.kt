package no.nav.modia.soknadstatus

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import no.nav.modia.soknadstatus.SoknadstatusDomain.Soknadstatus
import no.nav.modia.soknadstatus.SoknadstatusDomain.SoknadstatusOppdatering
import no.nav.modia.soknadstatus.SoknadstatusDomain.Soknadstatuser

fun Application.soknadstatusModule() {
    val config = Configuration()

    install(BaseNaisApp)
    install(KafkaStreamPlugin) {
        appname = config.appname
        brokerUrl = config.brokerUrl
        topology {
            stream<String, String>(config.sourceTopic)
                .mapValues(::deserialize)
                .filter(::filter)
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

fun deserialize(key: String?, value: String): SoknadstatusOppdatering {
    return Json.decodeFromString(value)
}

fun filter(key: String?, value: SoknadstatusOppdatering): Boolean {
    return true
}

fun persist(bytes: String?, soknadstatusOppdatering: SoknadstatusOppdatering) {
    println(soknadstatusOppdatering)
    repository.upsert(soknadstatusOppdatering)
}
