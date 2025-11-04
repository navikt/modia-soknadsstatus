package no.nav.modia.soknadsstatus

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.common.types.identer.Fnr
import no.nav.modia.soknadsstatus.infratructure.naudit.Audit
import no.nav.modia.soknadsstatus.infratructure.naudit.AuditResources
import no.nav.modiapersonoversikt.infrastructure.naudit.AuditIdentifier
import no.nav.personoversikt.common.ktor.utils.Security
import no.nav.personoversikt.common.logging.TjenestekallLogg

internal fun Application.installRouting(
    security: Security,
    services: Services,
) {
    routing {
        authenticate(*security.authproviders) {
            route("api") {
                route("soknadsstatus") {
                    route("behandling") {
                        get("{ident}") {
                            val ident = call.getIdent()
                            call.respondWithBehandling(ident, services)
                        }
                        post {
                            val ident = call.getIdentFromBody() ?: return@post call.respond(HttpStatusCode.NotFound)
                            call.respondWithBehandling(ident, services)
                        }
                    }
                    route("hendelse") {
                        get("{ident}") {
                            val ident = call.getIdent()
                            call.respondWithHendelser(ident, services)
                        }
                        post {
                            val ident = call.getIdentFromBody() ?: return@post call.respond(HttpStatusCode.NotFound)
                            call.respondWithHendelser(ident, services)
                        }
                    }
                }
            }
        }
    }
}

private fun ApplicationCall.getIdent(): String =
    this.parameters["ident"] ?: throw HttpStatusException(
        HttpStatusCode.BadRequest,
        "ident missing in request",
    )

private fun ApplicationCall.getUserToken(): String =
    this.principal<Security.SubjectPrincipal>()?.token?.removeBearerFromToken()
        ?: throw IllegalStateException("Ingen gyldig token funnet")

private suspend fun ApplicationCall.getIdentFromBody(): String? {
    try {
        val body = this.receiveText()
        val identDTO = Json { ignoreUnknownKeys = true }.decodeFromString(IdentDTO.serializer(), body)
        return identDTO.fnr
    } catch (e: Exception) {
        TjenestekallLogg.error("Klarte ikke å finne fnr i kall", fields = mapOf(), throwable = e)
        return null
    }
}

@Serializable
data class IdentDTO(
    val fnr: String,
)

private suspend fun ApplicationCall.respondWithBehandling(
    ident: String,
    services: Services,
) {
    val kabac = services.accessControl.buildKabac(this.authentication)
    return this.respondWithResult(
        kabac.check(services.policies.tilgangTilBrukerV2(Fnr(ident))).get(
            Audit.describe(
                this.authentication,
                Audit.Action.READ,
                AuditResources.Person.SakOgBehandling.Les,
                AuditIdentifier.FNR to ident,
            ),
        ) {
            runCatching {
                runBlocking {
                    if (request.queryParameters["inkluderHendelser"].toBoolean()) {
                        services.behandlingService.getAllForIdentWithHendelser(
                            userToken = getUserToken(),
                            ident,
                        )
                    } else {
                        services.behandlingService.getAllForIdent(
                            userToken = getUserToken(),
                            ident,
                        )
                    }
                }
            }
        },
    )
}

private suspend fun ApplicationCall.respondWithHendelser(
    ident: String,
    services: Services,
) {
    val kabac = services.accessControl.buildKabac(this.authentication)
    this.respondWithResult(
        kabac.check(services.policies.tilgangTilBrukerV2(Fnr(ident))).get(
            Audit.describe(
                this.authentication,
                Audit.Action.READ,
                AuditResources.Person.SakOgBehandling.Les,
                AuditIdentifier.FNR to ident,
            ),
        ) {
            runCatching {
                runBlocking {
                    services.hendelseService.getAllForIdent(
                        userToken = getUserToken(),
                        ident,
                    )
                }
            }
        },
    )
}
