package no.nav.modia.soknadsstatus

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import no.nav.modia.soknadsstatus.pdl.PdlOppslagServiceImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.assertThrows

val aktorIder = listOf("10108000398", "2222222222")
val oppdatering = SoknadsstatusDomain.SoknadsstatusInnkommendeOppdatering(
    aktorIder = aktorIder,
    behandlingsId = "12345",
    systemRef = "test",
    tema = "tema",
    status = SoknadsstatusDomain.Status.FERDIG_BEHANDLET,
    tidspunkt = Clock.System.now(),
)

@TestInstance(Lifecycle.PER_METHOD)
class SoknadsstatusServiceTest {
    var pdlOppslagServiceImpl = mockk<PdlOppslagServiceImpl>()
    private var repository = MockRepository()
    lateinit var soknadsstatusService: SoknadsstatusService

    @BeforeEach
    fun setUp() {
        repository = MockRepository()
        soknadsstatusService =
            SoknadsstatusServiceImpl(pdlOppslagService = pdlOppslagServiceImpl, repository = repository)
    }

    @Test
    fun `skal lagre alle aktørider i databasen`() {
        coEvery { pdlOppslagServiceImpl.hentFnrMedSystemToken(any()) }.returnsMany(aktorIder.first(), aktorIder[1])

        runBlocking {
            soknadsstatusService.fetchIdentsAndPersist(oppdatering)
        }
        assertEquals(
            2,
            repository.getAll().size,
            "PdlOppslagService lagret ikke status oppdatering for begge aktør ider",
        )
        val firstElement = soknadsstatusService.fetchDataForIdent(aktorIder.first())
        assertNotNull(firstElement, "Manglet soknadsstatus for ident")
    }

    @Test
    fun `skal kaste feil om kall til pdl feiler`() {
        coEvery { pdlOppslagServiceImpl.hentFnrMedSystemToken(any()) }.throws(IllegalStateException("Dummy exception"))
        assertThrows<IllegalStateException> {
            runBlocking {
                soknadsstatusService.fetchIdentsAndPersist(oppdatering)
            }
        }
    }
}

private class MockRepository : SoknadsstatusRepository {
    private val db = mutableMapOf<String, SoknadsstatusDomain.SoknadsstatusOppdatering>()

    private fun getKey(soknadsstatusOppdatering: SoknadsstatusDomain.SoknadsstatusOppdatering) =
        "${soknadsstatusOppdatering.behandlingsId}-${soknadsstatusOppdatering.ident}"

    private fun storeSoknadsstatusOppdatering(soknadsstatusOppdatering: SoknadsstatusDomain.SoknadsstatusOppdatering) {
        val key = getKey(soknadsstatusOppdatering)
        db[key] = soknadsstatusOppdatering
    }

    override fun get(ident: String): Result<List<SoknadsstatusDomain.SoknadsstatusOppdatering>> {
        return Result.success(db.values.filter { it.ident == ident })
    }

    override fun upsert(state: SoknadsstatusDomain.SoknadsstatusOppdatering): Result<Boolean> {
        println(state.ident)
        storeSoknadsstatusOppdatering(state)
        return Result.success(true)
    }

    fun getAll() = db.values.toList()
}
