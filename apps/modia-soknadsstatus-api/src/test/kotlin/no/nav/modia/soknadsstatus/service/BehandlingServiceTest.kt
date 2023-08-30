package no.nav.modia.soknadsstatus.service

import kotlinx.coroutines.runBlocking
import no.nav.modia.soknadsstatus.SoknadsstatusDomain
import no.nav.modia.soknadsstatus.TestUtilsWithDataSource
import no.nav.modia.soknadsstatus.pdl.PdlOppslagServiceMock
import no.nav.modia.soknadsstatus.repository.BehandlingRepository
import no.nav.modia.soknadsstatus.repository.BehandlingRepositoryImpl
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
    fun test() = runBlocking {
        service.upsert(behandling = SoknadsstatusDomain.BehandlingDAO(status = SoknadsstatusDomain.Status.UNDER_BEHANDLING))

        val result = service.getAllForIdent("mock-token", "1010800398")
    }
}