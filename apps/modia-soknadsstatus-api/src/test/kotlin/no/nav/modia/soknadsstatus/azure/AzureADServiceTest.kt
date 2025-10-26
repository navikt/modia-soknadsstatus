package no.nav.modia.soknadsstatus.azure

import io.mockk.every
import io.mockk.mockk
import no.nav.common.client.msgraph.AdGroupData
import no.nav.common.client.msgraph.AdGroupFilter
import no.nav.common.client.msgraph.MsGraphClient
import no.nav.common.types.identer.AzureObjectId
import no.nav.common.types.identer.EnhetId
import no.nav.modia.soknadsstatus.ansatt.AnsattRolle
import no.nav.modia.soknadsstatus.ansatt.RolleListe
import no.nav.modia.soknadsstatus.utils.BoundedOnBehalfOfTokenClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class AzureADServiceTest {
    val ident = "test-ident"
    val userToken = "test-token"
    private val msGraphClient: MsGraphClient = mockk<MsGraphClient>()
    private val tokenClient: BoundedOnBehalfOfTokenClient = mockk<BoundedOnBehalfOfTokenClient>()
    private val azureADService = AzureADServiceImpl(msGraphClient, tokenClient)

    @BeforeEach
    fun setUp() {
        every { tokenClient.exchangeOnBehalfOfToken(userToken) } returns userToken
    }

    @Test
    fun `skal returnere en liste av roller for gyldig ident`() {
        val roller = listOf(createGroupData("0000-GA-TEMA_OPP"), createGroupData("0000-GA-TEMA_AAP"))
        val forventedeRoller = listOf("0000-GA-TEMA_OPP", "0000-GA-TEMA_AAP")
        every { msGraphClient.hentAdGroupsForUser(userToken, ident) } returns roller

        val faktiskeRoller = azureADService.hentRollerForVeileder(ident, userToken)

        assertEquals(forventedeRoller, faktiskeRoller)
    }

    @Test
    fun `skal returnere en liste av EnhetId`() {
        val roller = listOf(createGroupData("0000-GA-ENHET_112"), createGroupData("0000-GA-ENHET_113"))
        val forventedeEnheter = listOf(EnhetId("112"), EnhetId("113"))
        every { msGraphClient.hentAdGroupsForUser(userToken, ident, AdGroupFilter.ENHET) } returns roller

        val faktiskeEnheter = azureADService.hentEnheterForVeileder(ident, userToken)

        assertEquals(forventedeEnheter, faktiskeEnheter)
    }

    @Test
    fun `skal returnere snittet av roller`() {
        val groupId = AzureObjectId(UUID.randomUUID().toString())
        val gitteRoller = RolleListe(setOf(AnsattRolle("0000-GA-KODE7", groupId)))
        val roller = listOf(createGroupData("0000-GA-TEMA_OPP"), createGroupData("0000-GA-KODE7", groupId))
        every { msGraphClient.hentAdGroupsForUser(userToken, ident) } returns roller
        val faktiskeSnitt = azureADService.hentIntersectRollerForVeileder(ident, userToken, gitteRoller)

        assertEquals(gitteRoller, faktiskeSnitt)
    }

    @Test
    fun `skal filtrere og returnere kun roller som starter med TEMA_ prefiks`() {
        val roller = listOf(createGroupData("0000-GA-TEMA_OPP"), createGroupData("0000-GA-TEMA_AAP"))
        val forventedeTemaer = listOf("OPP", "AAP")
        every { msGraphClient.hentAdGroupsForUser(userToken, ident, AdGroupFilter.TEMA) } returns roller

        val faktiskeTemaRoller = azureADService.hentTemaerForVeileder(ident, userToken)

        assertEquals(forventedeTemaer, faktiskeTemaRoller)
    }

    private fun createGroupData(
        displayName: String,
        groupId: AzureObjectId? = AzureObjectId(UUID.randomUUID().toString()),
    ) = AdGroupData(groupId, displayName)
}
