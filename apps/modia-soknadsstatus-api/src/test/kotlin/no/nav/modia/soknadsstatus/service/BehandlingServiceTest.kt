package no.nav.modia.soknadsstatus.service

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import no.nav.modia.soknadsstatus.InnkommendeHendelse
import no.nav.modia.soknadsstatus.SoknadsstatusDomain
import no.nav.modia.soknadsstatus.TestUtilsWithDataSource
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class BehandlingServiceTest : TestUtilsWithDataSource() {

    @BeforeEach
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun `opprette behandling`() = runBlocking {
        val behandling = SoknadsstatusDomain.Behandling(
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

        behandlingService.upsert(dataSource.connection, behandling = behandling)

        val result = behandlingService.getByBehandlingId("1500oVFWi")
        assertNotNull(result)
        assertEquals(result?.behandlingId, "1500oVFWi")
        assertEquals(result?.sakstema, "DAGP")
        assertEquals(result?.status, SoknadsstatusDomain.Status.FERDIG_BEHANDLET)
    }
    @Test
    fun `skal hente behandling med tilhørende hendelser`() = runBlocking {
            val identer = listOf("19099531196", "26127338824")


            val hendelseEn = InnkommendeHendelse(
                aktoerer = listOf(),
                identer = identer,
                ansvarligEnhet = "9958",
                behandlingsId = "1500oVFWi",
                behandlingsTema = "",
                behandlingsType = "",
                hendelsesType = SoknadsstatusDomain.HendelseType.BEHANDLING_OPPRETTET,
                hendelsesId = "50090284",
                hendelsesTidspunkt = LocalDateTime.parse("2023-06-16T11:40:24"),
                hendelsesProdusent = "AO01",
                sakstema = "DAGP",
                status = SoknadsstatusDomain.Status.UNDER_BEHANDLING,
            )

            val hendelseTo = InnkommendeHendelse(
                aktoerer = listOf(),
                identer = identer,
                ansvarligEnhet = "9958",
                behandlingsId = "1500oVFWi",
                behandlingsTema = "",
                behandlingsType = "",
                hendelsesType = SoknadsstatusDomain.HendelseType.BEHANDLING_OPPRETTET,
                hendelsesId = "1234567",
                hendelsesTidspunkt = LocalDateTime.parse("2023-06-16T12:40:24"),
                hendelsesProdusent = "AO01",
                sakstema = "DAGP",
                status = SoknadsstatusDomain.Status.UNDER_BEHANDLING,
            )

            hendelseService.onNewHendelse(hendelseEn)
            hendelseService.onNewHendelse(hendelseTo)

            val result = behandlingService.getAllForIdentsWithHendelser(identer)

            assertEquals(1, result.size)
            val behandling = result.first()
            assertEquals(2, behandling.hendelser?.size)
        }
}
