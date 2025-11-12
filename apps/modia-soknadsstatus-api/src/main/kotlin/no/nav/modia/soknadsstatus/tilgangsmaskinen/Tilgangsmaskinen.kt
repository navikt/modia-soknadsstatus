package no.nav.modia.soknadsstatus.tilgangsmaskinen

import com.github.benmanes.caffeine.cache.Cache
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.modia.soknadsstatus.consumer.tilgangsmaskinen.generated.infrastructure.*
import no.nav.modia.soknadsstatus.utils.CacheUtils
import no.nav.personoversikt.common.logging.TjenestekallLogg
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

interface Tilgangsmaskinen {
    fun sjekkTilgang(
        veilederIdent: NavIdent,
        fnr: Fnr,
    ): TilgangsMaskinResponse?
}

data class TilgangsMaskinResponse(
    val harTilgang: Boolean,
)

open class TilgangsmaskinenImpl(
    private val baseUrl: String,
    private val client: OkHttpClient,
    private val cache: Cache<String, TilgangsMaskinResponse?> = CacheUtils.createCache(),
) : Tilgangsmaskinen {
    override fun sjekkTilgang(
        veilederIdent: NavIdent,
        fnr: Fnr,
    ): TilgangsMaskinResponse? =
        cache.get("${veilederIdent.get()}-${fnr.get()}") {
            try {
                val requestBody = fnr.get().toRequestBody("application/json".toMediaTypeOrNull())
                val response =
                    client
                        .newCall(
                            Request
                                .Builder()
                                .url("$baseUrl/api/v1/ccf/komplett/${veilederIdent.get()}")
                                .post(requestBody)
                                .build(),
                        ).execute()

                return@get when {
                    response.isSuccessful -> TilgangsMaskinResponse(harTilgang = true)
                    response.isClientError -> {
                        logErrorBasedOnResponse(response, veilederIdent.get(), fnr.get())
                        TilgangsMaskinResponse(harTilgang = false)
                    }
                    response.isServerError -> {
                        logErrorBasedOnResponse(response, veilederIdent.get(), fnr.get())
                        TilgangsMaskinResponse(harTilgang = false)
                    }

                    else -> {
                        TjenestekallLogg.error(
                            header = "UnsupportedOperationException ved tilgang sjekking fra tilgangsmaskin",
                            fields =
                                mapOf(
                                    "veilederIdent" to veilederIdent,
                                    "fnr" to fnr,
                                    "message" to response.message,
                                ),
                        )
                        null
                    }
                }
            } catch (e: Exception) {
                TjenestekallLogg.error(
                    "Greide ikke å hente tilgang fra tilgangsmaskinen",
                    throwable = e,
                    fields =
                        mapOf(
                            "veilederIdent" to veilederIdent,
                            "fnr" to fnr,
                        ),
                )
                return@get null
            }
        }

    private fun logErrorBasedOnResponse(
        response: okhttp3.Response,
        veilederIdent: String,
        fnr: String,
    ) {
        val errorHeader =
            if (response.isClientError) {
                "ClientException ved tilgang sjekking fra tilgangsmaskin"
            } else {
                "ServerException ved tilgang sjekking fra tilgangsmaskin"
            }

        TjenestekallLogg.error(
            header = errorHeader,
            fields =
                mapOf(
                    "veilederIdent" to veilederIdent,
                    "fnr" to fnr,
                    "message" to response.message,
                ),
        )
    }
}
