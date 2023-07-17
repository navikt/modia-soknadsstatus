package no.nav.modia.soknadsstatus.pdl

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.google.common.testing.FakeTicker
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.api.generated.pdl.enums.IdentGruppe
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.modia.soknadsstatus.MockData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

internal class PdlCacheTest {
    private val pdlClient = mockk<PdlClientImpl>()
    private val fnrCache: Cache<String, String> = getCache()

    private val pdlService =
        PdlOppslagServiceImpl(pdlClient, fnrCache)

    private val ident = NavIdent("Z999999")
    private val fnr = Fnr("10108000398")
    private val aktorId = AktorId("987654321987")
    private val token = JWT.create().withSubject(ident.get()).sign(Algorithm.none())

    companion object {
        private val ticker = FakeTicker()
        fun <VALUE_TYPE> getCache(): Cache<String, VALUE_TYPE> = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(1000)
            .ticker(ticker::read)
            .build()
    }

    @Test
    internal fun `Skal ikke kalle pdl hvis fnr finnes i cache`() {
        fnrCache.put(aktorId.toString(), fnr.toString())
        val fnrValue = runBlocking { pdlService.hentFnr(token, aktorId.toString()) }

        verify(exactly = 0) { runBlocking { pdlClient.execute(any(), any()) } }
        assertEquals(fnrValue, fnr.toString())
    }

    @Test
    internal fun `Skal kalle pdl hvis fnr ikke finnes i cache`() {
        fnrCache.put(aktorId.toString(), MockData.bruker.fnr)
        ticker.advance(30, TimeUnit.MINUTES)
        coEvery {
            pdlClient.hentAktivIdent(token, aktorId.toString(), IdentGruppe.FOLKEREGISTERIDENT)
        } answers {
            fnr.toString()
        }
        val fnrValue = runBlocking { pdlService.hentFnr(token, aktorId.toString()) }

        verify(exactly = 1) {
            runBlocking {
                pdlClient.hentAktivIdent(
                    token,
                    aktorId.toString(),
                    IdentGruppe.FOLKEREGISTERIDENT,
                )
            }
        }
        assertEquals(fnrValue, fnr.toString())
    }
}
