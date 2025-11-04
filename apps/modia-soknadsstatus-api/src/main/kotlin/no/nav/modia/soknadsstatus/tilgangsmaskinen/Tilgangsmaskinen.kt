package no.nav.modia.soknadsstatus.tilgangsmaskinen

import com.github.benmanes.caffeine.cache.Cache
import io.ktor.server.auth.*
import no.nav.common.health.HealthCheckUtils
import no.nav.common.health.selftest.SelfTestCheck
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.UrlUtils
import no.nav.modia.soknadsstatus.consumer.tilgangsmaskinen.generated.apis.TilgangControllerApi
import no.nav.modia.soknadsstatus.consumer.tilgangsmaskinen.generated.infrastructure.*
import no.nav.modia.soknadsstatus.infratructure.ping.Pingable
import no.nav.modia.soknadsstatus.utils.CacheUtils
import no.nav.personoversikt.common.logging.TjenestekallLogg
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory

interface Tilgangsmaskinen : Pingable {
    fun sjekkTilgang(fnr: Fnr): TilgangsMaskinResponse?
}

data class TilgangsMaskinResponse(
    val harTilgang: Boolean,
    val error: ForbiddenResponse? = null,
)

open class TilgangsmaskinenImpl(
    private val url: String,
    private val client: OkHttpClient,
    private val cache: Cache<String, TilgangsMaskinResponse?> = CacheUtils.createCache(),
) : Tilgangsmaskinen {
    private val tilgangsMaskinenApi = TilgangControllerApi(url, client)
    private val logger = LoggerFactory.getLogger(TilgangsmaskinenImpl::class.java)

    override fun sjekkTilgang(fnr: Fnr): TilgangsMaskinResponse? =
        cache.get(fnr.get()) {
            try {
                tilgangsMaskinenApi.kompletteRegler(fnr.get())
                TilgangsMaskinResponse(harTilgang = true)
            } catch (e: ClientException) {
                makeErrorResponse(e)
            } catch (e: ServerException) {
                TjenestekallLogg.error("ServerException", throwable = e, fields = mapOf("fnr" to fnr.get()))
                TilgangsMaskinResponse(harTilgang = false)
            } catch (e: Exception) {
                TjenestekallLogg.error(
                    "Greide ikke å hente tilgang fra tilgangsmaskinen",
                    throwable = e,
                    fields = mapOf("fnr" to fnr.get()),
                )
                null
            }
        }

    private fun makeErrorResponse(response: ClientException): TilgangsMaskinResponse {
        if (response.statusCode == 403) {
            response as ClientError<*>
            try {
                return TilgangsMaskinResponse(harTilgang = false)
            } catch (e: Exception) {
                logger.warn("Parse exception when parsing error response from tilgangsmaskinen: ${e.message}")
                return TilgangsMaskinResponse(harTilgang = false)
            }
        }
        logger.warn("Received error response from tilgangsmaskinen: ${response.statusCode}")
        return TilgangsMaskinResponse(harTilgang = false)
    }

    override fun ping() =
        SelfTestCheck(
            "pdl-pip-api via $url",
            false,
        ) {
            HealthCheckUtils.pingUrl(UrlUtils.joinPaths(url, "/internal/health/liveness"), client)
        }
}
