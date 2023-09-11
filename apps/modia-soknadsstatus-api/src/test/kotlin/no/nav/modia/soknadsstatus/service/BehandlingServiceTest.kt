package no.nav.modia.soknadsstatus.service

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import no.nav.modia.soknadsstatus.SoknadsstatusDomain
import no.nav.modia.soknadsstatus.TestUtilsWithDataSource
import no.nav.modia.soknadsstatus.pdl.PdlOppslagServiceMock
import no.nav.modia.soknadsstatus.repository.BehandlingRepositoryImpl
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class BehandlingServiceTest : TestUtilsWithDataSource() {
    lateinit var service: BehandlingService
    val pdl = PdlOppslagServiceMock()

    @BeforeEach
    override fun setUp() {
        val repository = BehandlingRepositoryImpl(dataSource)
        service = BehandlingServiceImpl(repository, pdl)
        super.setUp()
    }

    @Test
    fun `opprette behandling`() = runBlocking {
        val behandling = SoknadsstatusDomain.BehandlingDAO(
            behandlingId = "1500oVFWi",
            produsentSystem = "A100",
            startTidspunkt = LocalDateTime.parse("2023-06-16T11:40:24"),
            sluttTidspunkt = LocalDateTime.parse("2023-06-26T11:40:24"),
            sistOppdatert = LocalDateTime.parse("2023-06-26T11:40:24"),
            sakstema = "DAGP",
            behandlingsTema = "DAGP",
            behandlingsType = "",
            status = SoknadsstatusDomain.Status.FERDIG_BEHANDLET,
            ansvarligEnhet = "9958",
            primaerBehandling = "",
        )

        service.upsert(dataSource.connection, behandling = behandling)

        val result = service.getByBehandlingId("1500oVFWi")
        assertNotNull(result)
        assertEquals(result?.behandlingId, "1500oVFWi")
        assertEquals(result?.sakstema, "DAGP")
        assertEquals(result?.status, SoknadsstatusDomain.Status.FERDIG_BEHANDLET)
    }
}
