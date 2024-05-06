package no.nav.modia.soknadsstatus.pdlpip

import com.github.benmanes.caffeine.cache.Cache
import no.nav.api.generated.pdl.enums.AdressebeskyttelseGradering
import no.nav.api.generated.pdl.hentadressebeskyttelse.Adressebeskyttelse
import no.nav.common.health.HealthCheckUtils
import no.nav.common.health.selftest.SelfTestCheck
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.UrlUtils
import no.nav.modia.soknadsstatus.consumer.pdlPipApi.generated.apis.PIPTjenesteForPDLDataKunForSystemerApi
import no.nav.modia.soknadsstatus.consumer.pdlPipApi.generated.models.PipAdressebeskyttelse
import no.nav.modia.soknadsstatus.consumer.pdlPipApi.generated.models.PipPersondataResponse
import no.nav.modia.soknadsstatus.infratructure.ping.Pingable
import no.nav.modia.soknadsstatus.utils.CacheUtils
import okhttp3.OkHttpClient

interface PdlPipApi : Pingable {
    fun hentPdlPipPerson(fnr: Fnr): PipPersondataResponse
}

class PdlPipApiImpl(
    private val url: String,
    private val client: OkHttpClient,
    private val cache: Cache<Fnr, PipPersondataResponse> = CacheUtils.createCache(),
) : PdlPipApi {
    private val pdlPipApi = PIPTjenesteForPDLDataKunForSystemerApi(url, client)

    override fun hentPdlPipPerson(fnr: Fnr) =
        requireNotNull(
            cache.get(fnr) {
                pdlPipApi.lookupIdent(fnr.get())
            },
        )

    override fun ping() =
        SelfTestCheck(
            "pdl-pip-api via $url",
            false,
        ) {
            HealthCheckUtils.pingUrl(UrlUtils.joinPaths(url, "/internal/health/liveness"), client)
        }
}

fun PipAdressebeskyttelse.toPdlAdresseBeskyttelse() =
    Adressebeskyttelse(
        gradering =
            when (this.gradering) {
                "STRENGT_FORTROLIG_UTLAND" -> AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
                "STRENGT_FORTROLIG" -> AdressebeskyttelseGradering.STRENGT_FORTROLIG
                "FORTROLIG" -> AdressebeskyttelseGradering.FORTROLIG
                "UGRADERT" -> AdressebeskyttelseGradering.UGRADERT
                else -> AdressebeskyttelseGradering.__UNKNOWN_VALUE
            },
    )
