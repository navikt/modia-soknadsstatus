package no.nav.modia.soknadsstatus.pdl

import io.ktor.client.request.*
import io.ktor.server.auth.*
import kotlinx.coroutines.runBlocking
import no.nav.api.generated.pdl.HentAdressebeskyttelse
import no.nav.api.generated.pdl.HentAktorid
import no.nav.api.generated.pdl.HentGeografiskTilknyttning
import no.nav.api.generated.pdl.enums.IdentGruppe
import no.nav.api.generated.pdl.hentadressebeskyttelse.Adressebeskyttelse
import no.nav.modia.soknadsstatus.accesscontrol.RestConstants
import no.nav.modia.soknadsstatus.utils.BoundedMachineToMachineTokenClient
import no.nav.modia.soknadsstatus.utils.BoundedOnBehalfOfTokenClient
import no.nav.utils.getCallId

class PdlOppslagServiceImpl(
    private val pdlClient: PdlClient,
    private val oboTokenProvider: BoundedOnBehalfOfTokenClient,
    private val machineToMachineTokenClient: BoundedMachineToMachineTokenClient,
) : PdlOppslagService {

//    private fun requestConfig(token: String): HeadersBuilder = {
//        val oboToken = oboTokenProvider.exchangeOnBehalfOfToken(token)
//        header("Authorization", "Bearer $oboToken")
//        header("Tema", "GEN")
//        header("X-Correlation-ID", getCallId())
//    }

    override fun hentFnr(userToken: String, aktorId: String): String? = hentAktivIdent(aktorId, IdentGruppe.FOLKEREGISTERIDENT, userTokenAuthorizationHeaders(userToken))
    override fun hentFnrMedSystemToken(aktorId: String): String? = hentAktivIdent(aktorId, IdentGruppe.FOLKEREGISTERIDENT, systemTokenAuthorizationHeaders)
    override fun hentAktorId(userToken: String, fnr: String): String? = hentAktivIdent(fnr, IdentGruppe.AKTORID, userTokenAuthorizationHeaders(userToken))

    override fun hentGeografiskTilknytning(userToken: String, fnr: String): String? = runBlocking {
        pdlClient.execute(
            HentGeografiskTilknyttning(HentGeografiskTilknyttning.Variables(fnr)),
            userTokenAuthorizationHeaders(userToken)
        )
            .data
            ?.hentGeografiskTilknytning
            ?.run {
                gtBydel ?: gtKommune ?: gtLand
            }
    }

    override fun hentAdresseBeskyttelse(userToken: String, fnr: String): List<Adressebeskyttelse> = runBlocking {
        pdlClient.execute(HentAdressebeskyttelse(HentAdressebeskyttelse.Variables(fnr)), userTokenAuthorizationHeaders(userToken))
            .data?.hentPerson?.adressebeskyttelse
            ?: emptyList()
    }

    private fun userTokenAuthorizationHeaders(userToken: String): HeadersBuilder = {
        val exchangedToken = oboTokenProvider.exchangeOnBehalfOfToken(userToken)
        header(
            RestConstants.AUTHORIZATION,
            RestConstants.AUTH_METHOD_BEARER + RestConstants.AUTH_SEPERATOR + exchangedToken
        )
        header(RestConstants.TEMA_HEADER, RestConstants.ALLE_TEMA_HEADERVERDI)
        header("X-Correlation-ID", getCallId())
    }

    private val systemTokenAuthorizationHeaders: HeadersBuilder = {
        val systemuserToken: String = machineToMachineTokenClient.createMachineToMachineToken()
        header(
            RestConstants.AUTHORIZATION,
            RestConstants.AUTH_METHOD_BEARER + RestConstants.AUTH_SEPERATOR + systemuserToken
        )
        header(RestConstants.TEMA_HEADER, RestConstants.ALLE_TEMA_HEADERVERDI)
        header("X-Correlation-ID", getCallId())
    }

    private fun hentAktivIdent(ident: String, gruppe: IdentGruppe, requestCustomizer: HeadersBuilder): String? = runBlocking {
        pdlClient.execute(HentAktorid(HentAktorid.Variables(ident, listOf(gruppe))), requestCustomizer)
            .data
            ?.hentIdenter
            ?.identer
            ?.firstOrNull()
            ?.ident
    }
}
