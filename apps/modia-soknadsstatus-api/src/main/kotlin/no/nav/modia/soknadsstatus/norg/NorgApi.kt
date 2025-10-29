package no.nav.modia.soknadsstatus.norg

import kotlinx.coroutines.runBlocking
import no.nav.common.health.HealthCheckResult
import no.nav.common.health.selftest.SelfTestCheck
import no.nav.common.types.identer.EnhetId
import no.nav.modia.soknadsstatus.consumer.norg.generated.apis.ArbeidsfordelingApi
import no.nav.modia.soknadsstatus.consumer.norg.generated.apis.EnhetApi
import no.nav.modia.soknadsstatus.consumer.norg.generated.apis.EnhetskontaktinfoApi
import no.nav.modia.soknadsstatus.consumer.norg.generated.apis.OrganiseringApi
import no.nav.modia.soknadsstatus.consumer.norg.generated.models.*
import no.nav.modia.soknadsstatus.infratructure.ping.Pingable
import no.nav.modia.soknadsstatus.utils.CacheUtils
import no.nav.personoversikt.common.utils.Retry
import no.nav.personoversikt.common.utils.StringUtils.isNumeric
import okhttp3.OkHttpClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

interface NorgApi : Pingable {
    companion object {
        @JvmStatic
        val IKKE_NEDLAGT: List<NorgDomain.EnhetStatus> =
            NorgDomain
                .EnhetStatus
                .values()
                .asList()
                .minus(NorgDomain.EnhetStatus.NEDLAGT)
    }

    fun finnNavKontor(
        geografiskTilknytning: String,
        diskresjonskode: NorgDomain.DiskresjonsKode?,
    ): NorgDomain.Enhet?

    fun hentRegionalEnheter(enhet: List<EnhetId>): List<EnhetId>

    fun hentRegionalEnhet(enhet: EnhetId): EnhetId?
}

class NorgApiImpl(
    private val url: String,
    private val consumerId: String,
    httpClient: OkHttpClient,
    scheduler: Timer = Timer(),
    private val clock: Clock = Clock.systemDefaultZone(),
) : NorgApi {
    private val log: Logger = LoggerFactory.getLogger(NorgApi::class.java)
    private val cacheRetention = 1.hours
    private val cacheGraceperiod = Duration.ofMinutes(2)
    private var cache: Map<EnhetId, NorgDomain.EnhetKontaktinformasjon> = emptyMap()
    private var lastUpdateOfCache: LocalDateTime? = null
    private val navkontorCache = createNorgCache<String, NorgDomain.Enhet>()
    private val gtCache = createNorgCache<String, List<NorgDomain.EnhetGeografiskTilknyttning>>()
    private val regionalkontorCache = createNorgCache<EnhetId, EnhetId>()

    private val retry =
        Retry(
            Retry.Config(
                initDelay = 30.seconds,
                growthFactor = 2.0,
                delayLimit = 1.hours,
                scheduler = scheduler,
            ),
        )

    private val arbeidsfordelingApi = ArbeidsfordelingApi(url, httpClient)
    private val enhetApi = EnhetApi(url, httpClient)
    private val enhetKontaktInfoApi = EnhetskontaktinfoApi(url, httpClient)
    private val organiseringApi = OrganiseringApi(url, httpClient)

    init {
        hentEnheterOgKontaktinformasjon()
        scheduler.scheduleAtFixedRate(
            delay = cacheRetention.inWholeMilliseconds,
            period = cacheRetention.inWholeMilliseconds,
            action = {
                hentEnheterOgKontaktinformasjon()
            },
        )
    }

    override fun finnNavKontor(
        geografiskTilknytning: String,
        diskresjonskode: NorgDomain.DiskresjonsKode?,
    ): NorgDomain.Enhet? {
        val key = "finnNavKontor[$geografiskTilknytning,$diskresjonskode]"
        return navkontorCache.get(key) {
            if (geografiskTilknytning.isNumeric()) {
                enhetApi
                    .getEnhetByGeografiskOmraadeUsingGET(
                        geografiskOmraade = geografiskTilknytning,
                        disk = diskresjonskode?.name,
                    ).let(::toInternalDomain)
            } else {
                /**
                 * Ikke numerisk GT tilsier at det er landkode pga utenlandsk GT og da har vi ingen enhet
                 */
                null
            }
        }
    }

    override fun hentRegionalEnheter(enhet: List<EnhetId>): List<EnhetId> = enhet.mapNotNull(::hentRegionalEnhet)

    override fun hentRegionalEnhet(enhet: EnhetId): EnhetId? =
        regionalkontorCache.get(enhet) { enhetId ->
            organiseringApi
                .getAllOrganiseringerForEnhetUsingGET(enhetId.get())
                .firstOrNull { it.orgType == "FYLKE" }
                ?.organiserer
                ?.nr
                ?.let(::EnhetId)
        }

    override fun ping() =
        SelfTestCheck(
            """NorgApi via $url (${cache.size}, ${gtCache.estimatedSize()}, ${navkontorCache.estimatedSize()},
               ${regionalkontorCache.estimatedSize()})
            """.trimMargin(),
            false,
        ) {
            val limit = LocalDateTime.now(clock).minus(cacheRetention.toJavaDuration()).plus(cacheGraceperiod)
            val cacheIsFresh = lastUpdateOfCache?.isAfter(limit) == true

            if (cacheIsFresh && cache.isNotEmpty()) {
                HealthCheckResult.healthy()
            } else {
                HealthCheckResult.unhealthy(
                    """
                    Last updated: $lastUpdateOfCache
                    CacheSize: ${cache.size}
                    """.trimIndent(),
                )
            }
        }

    private fun hentEnheterOgKontaktinformasjon() {
        runBlocking {
            retry.run {
                cache =
                    enhetKontaktInfoApi
                        .hentAlleEnheterInkludertKontaktinformasjonUsingGET(
                            consumerId = consumerId,
                        ).mapNotNull {
                            try {
                                toInternalDomain(it)
                            } catch (e: Exception) {
                                log.error("Kunne ikke mappe enhet til lokalt format. $it", e)
                                null
                            }
                        }.associateBy { EnhetId(it.enhet.enhetId) }
                lastUpdateOfCache = LocalDateTime.now(clock)
            }
        }
    }

    private fun <KEY, VALUE> createNorgCache() =
        CacheUtils.createCache<KEY, VALUE>(
            expireAfterWrite = cacheRetention,
            maximumSize = 2000,
        )

    companion object {
        internal fun toInternalDomain(kontor: RsNavKontorDTO) =
            NorgDomain.EnhetGeografiskTilknyttning(
                alternativEnhetId = kontor.alternativEnhetId?.toString(),
                enhetId = kontor.enhetId?.toString(),
                geografiskOmraade = kontor.geografiskOmraade,
                navKontorId = kontor.navKontorId?.toString(),
            )

        internal fun toInternalDomain(enhet: RsEnhetDTO) =
            NorgDomain.Enhet(
                enhetId = requireNotNull(enhet.enhetNr),
                enhetNavn = requireNotNull(enhet.navn),
                status = NorgDomain.EnhetStatus.safeValueOf(enhet.status),
                oppgavebehandler = requireNotNull(enhet.oppgavebehandler),
            )

        internal fun toInternalDomain(enhet: RsEnhetInkludertKontaktinformasjonDTO) =
            NorgDomain.EnhetKontaktinformasjon(
                enhet = toInternalDomain(requireNotNull(enhet.enhet)),
                publikumsmottak =
                    enhet.kontaktinformasjon?.publikumsmottak?.map { toInternalDomain(it) }
                        ?: emptyList(),
                overordnetEnhet = enhet.overordnetEnhet?.let(::EnhetId),
            )

        private fun toInternalDomain(mottak: RsPublikumsmottakDTO) =
            NorgDomain.Publikumsmottak(
                besoksadresse = mottak.besoeksadresse?.let { toInternalDomain(it) },
                apningstider =
                    mottak
                        .aapningstider
                        ?.filter { it.dag != null }
                        ?.map { toInternalDomain(it) } ?: emptyList(),
            )

        private fun toInternalDomain(adresse: RsStedsadresseDTO) =
            NorgDomain.Gateadresse(
                gatenavn = adresse.gatenavn,
                husnummer = adresse.husnummer,
                husbokstav = adresse.husbokstav,
                postnummer = adresse.postnummer,
                poststed = adresse.poststed,
            )

        private fun toInternalDomain(aapningstid: RsAapningstidDTO) =
            NorgDomain.Apningstid(
                ukedag = NorgDomain.Ukedag.safeValueOf(aapningstid.dag),
                stengt = aapningstid.stengt ?: false,
                apentFra = aapningstid.fra,
                apentTil = aapningstid.til,
            )
    }
}
