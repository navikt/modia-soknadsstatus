package no.nav.modia.soknadsstatus.pdl

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.common.testing.FakeTicker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.api.generated.pdl.enums.IdentGruppe
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.modia.soknadsstatus.SuspendCache
import no.nav.modia.soknadsstatus.SuspendCacheImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

internal class PdlCacheTest {
    private val pdlClient = mockk<PdlClientImpl>()
    private var fnrCache: SuspendCache<String, String?> = getCache()

    private var pdlService =
        PdlOppslagServiceImpl(pdlClient, fnrCache)

    private val ident = NavIdent("Z999999")
    private val fnr = Fnr("10108000398")
    private val aktorId = AktorId("987654321987")
    private val token = JWT.create().withSubject(ident.get()).sign(Algorithm.none())

    companion object {
        private val ticker = FakeTicker()
        fun <VALUE_TYPE> getCache(): SuspendCache<String, VALUE_TYPE> = SuspendCacheImpl(ticker = ticker::read)
    }

    @BeforeEach
    fun setUp() {
        fnrCache = getCache()
        pdlService = PdlOppslagServiceImpl(pdlClient, fnrCache)
    }

    @Test
    internal fun `Skal ikke kalle pdl hvis fnr finnes i cache`() {
        fnrCache.put(aktorId.toString(), fnr.toString())
        val fnrValue = runBlocking { pdlService.hentFnr(token, aktorId.toString()) }

        coVerify(exactly = 0) { pdlClient.hentAktivIdent(any(), any(), any()) }
        assertEquals(fnrValue, fnr.toString())
    }

    @Test
    internal fun `Skal kalle pdl hvis fnr ikke finnes i cache`() = runBlocking {
        ticker.advance(30, TimeUnit.MINUTES)
        coEvery {
            pdlClient.hentAktivIdent(token, aktorId.toString(), IdentGruppe.FOLKEREGISTERIDENT)
        } answers {
            fnr.toString()
        }

        val answer = pdlClient.hentAktivIdent(token, aktorId.toString(), IdentGruppe.FOLKEREGISTERIDENT)

        coVerify(exactly = 1) {
            pdlClient.hentAktivIdent(
                token,
                aktorId.toString(),
                IdentGruppe.FOLKEREGISTERIDENT,
            )
        }
        assertEquals(fnr.toString(), answer)
    }
}
