package no.nav.modia.soknadsstatus.pdl

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.api.generated.pdl.hentadressebeskyttelse.Adressebeskyttelse
import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.common.token_client.client.OnBehalfOfTokenClient
import no.nav.modia.soknadsstatus.AppMode
import no.nav.modia.soknadsstatus.utils.*
import java.net.URL
import java.util.concurrent.TimeUnit

object PdlConfig {
    fun factory(
        appMode: AppMode,
        pdlEnv: PdlEnv,
        oboTokenProvider: OnBehalfOfTokenClient,
        machineTokenClient: MachineToMachineTokenClient,
    ): PdlOppslagService {
        if (appMode == AppMode.NAIS) {
            val scope = pdlEnv.scope
            val url = pdlEnv.url
            val pdlClient = PdlClient(
                oboTokenProvider.bindTo(DownstreamApi.parse(scope)),
                machineTokenClient.bindTo((DownstreamApi.parse(scope))),
                URL(url),
            )
            val fnrCache: Cache<String, String> = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build()
            val aktorIdCache: Cache<String, String> = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build()
            val geografiskTilknytningCache: Cache<String, String> = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build()

            val adresseBeskyttelseCache: Cache<String, List<Adressebeskyttelse>> = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build()

            return PdlOppslagServiceImpl(
                pdlClient,
                fnrCache,
                aktorIdCache,
                geografiskTilknytningCache,
                adresseBeskyttelseCache,
            )
        }

        return PdlOppslagServiceMock()
    }
}

data class PdlEnv(
    val url: String,
    val scope: String,
)
