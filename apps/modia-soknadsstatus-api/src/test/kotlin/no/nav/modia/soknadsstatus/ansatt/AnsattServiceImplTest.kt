package no.nav.modia.soknadsstatus.ansatt

import io.mockk.every
import io.mockk.mockk
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.NavIdent
import no.nav.modia.soknadsstatus.azure.AzureADService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AnsattServiceImplTest {
    private val azureADService = mockk<AzureADService>()
    private val ansattServiceImpl: AnsattServiceImpl = AnsattServiceImpl(azureADService)

    @Test
    fun `skal hente alle ansatte for en enhet`() {
        every { azureADService.hentEnheterForVeileder("Z994404", "token") } returns
            listOf(EnhetId("111"), EnhetId("222"), EnhetId("333"))

        val enheter = ansattServiceImpl.hentEnhetsliste("token", NavIdent("Z994404"))
        assertEquals(enheter.size, 3)
    }

    @Test
    fun `skal kunne hente liste over ansatt sine fagområder`() {
        every { azureADService.hentTemaerForVeileder("Z994404", "token") } returns
            listOf("AAP", "DAG")

        val fagomraader = ansattServiceImpl.hentAnsattFagomrader("token", "Z994404")
        assertEquals(fagomraader.size, 2)
    }

    @Test
    fun `skal kunne hente roller for veileder`() {
        every { azureADService.hentRollerForVeileder("Z994404", "token") } returns
            listOf("0000-GA-TEMA_OPP", "0000-GA-TEMA_AAP", "0000-GA-ENHET_112")

        val roller = ansattServiceImpl.hentVeilederRoller("token", "Z994404")
        assertEquals(3, roller.size)
    }
}
