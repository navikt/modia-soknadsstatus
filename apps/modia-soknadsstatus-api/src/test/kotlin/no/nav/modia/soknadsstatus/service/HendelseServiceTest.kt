package no.nav.modia.soknadsstatus.service

import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import no.nav.modia.soknadsstatus.InnkommendeHendelse
import no.nav.modia.soknadsstatus.SoknadsstatusDomain
import no.nav.modia.soknadsstatus.TestUtilsWithDataSource
import no.nav.modia.soknadsstatus.repository.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class HendelseServiceTest : TestUtilsWithDataSource() {

    @BeforeEach
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun `opprette behandling og hendelse`() = runBlocking {
        val opprettHendelse = InnkommendeHendelse(
            aktoerer = listOf("1909953119612", "2612733882412"),
            identer = listOf("19099531196", "26127338824"),
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
            primaerBehandling = SoknadsstatusDomain.PrimaerBehandling("1500oVFWj", "forrige"),
        )

        every { runBlocking { pdlOppslagService.hentFnrMedSystemToken("1909953119612") } } returns "19099531196"
        every { runBlocking { pdlOppslagService.hentFnrMedSystemToken("2612733882412") } } returns "26127338824"
        every {
            runBlocking {
                pdlOppslagService.hentAktiveIdenter(
                    "mock-token",
                    "19099531196",
                )
            }
        } returns listOf("19099531196", "26127338824")

        hendelseService.onNewHendelse(innkommendeHendelse = opprettHendelse)

        val behandlingResult = behandlingService.getAllForIdent("mock-token", "19099531196")
        assertNotNull(behandlingResult)
        assertEquals(behandlingResult.size, 1)
        assertEquals("1500oVFWi", behandlingResult.first().behandlingId)
        assertEquals("DAGP", behandlingResult.first().sakstema)
        assertEquals("1500oVFWj", behandlingResult.first().primaerBehandlingId)
        assertEquals("forrige", behandlingResult.first().primaerBehandlingType)
        assertEquals(SoknadsstatusDomain.Status.UNDER_BEHANDLING, behandlingResult.first().status)

        val hendelseResult = hendelseService.getAllForIdent("mock-token", "19099531196")
        assertNotNull(hendelseResult)
        assertEquals(1, hendelseResult.size)
        assertEquals(behandlingResult.first().id, hendelseResult.first().modiaBehandlingId)
        assertEquals(behandlingResult.first().behandlingId, hendelseResult.first().behandlingId)
        assertEquals(behandlingResult.first().behandlingsType, hendelseResult.first().behandlingsType)
        assertEquals(behandlingResult.first().behandlingsTema, hendelseResult.first().behandlingsTema)
        assertEquals("50090284", hendelseResult.first().hendelseId)
        assertEquals(SoknadsstatusDomain.HendelseType.BEHANDLING_OPPRETTET, hendelseResult.first().hendelseType)
        assertEquals(SoknadsstatusDomain.Status.UNDER_BEHANDLING, hendelseResult.first().status)

        val avsluttHendelse = InnkommendeHendelse(
            aktoerer = listOf("1909953119612", "2612733882412"),
            identer = listOf("19099531196", "26127338824"),
            ansvarligEnhet = "9958",
            behandlingsId = "1500oVFWi",
            behandlingsTema = "",
            behandlingsType = "",
            hendelsesType = SoknadsstatusDomain.HendelseType.BEHANDLING_AVSLUTTET,
            hendelsesId = "50090286",
            hendelsesTidspunkt = LocalDateTime.parse("2023-06-16T11:40:24"),
            hendelsesProdusent = "AO01",
            sakstema = "DAGP",
            status = SoknadsstatusDomain.Status.FERDIG_BEHANDLET,
            primaerBehandling = SoknadsstatusDomain.PrimaerBehandling("1500oVFWj", "forrige"),
        )

        hendelseService.onNewHendelse(innkommendeHendelse = avsluttHendelse)

        val nyBehandlingResult = behandlingService.getAllForIdent("mock-token", "19099531196")
        assertNotNull(nyBehandlingResult)
        assertEquals(nyBehandlingResult.size, 1)
        assertEquals("1500oVFWi", nyBehandlingResult.first().behandlingId)
        assertEquals("DAGP", nyBehandlingResult.first().sakstema)
        assertEquals(SoknadsstatusDomain.Status.FERDIG_BEHANDLET, nyBehandlingResult.first().status)

        val nyHendelseResult = hendelseService.getAllForIdent("mock-token", "19099531196")
        assertNotNull(nyHendelseResult)
        assertEquals(2, nyHendelseResult.size)
    }

    @Test
    fun `opprette behandling og hendelse roll back`() = runBlocking {
        coEvery { pdlOppslagService.hentFnrMedSystemToken("1909953119612") } throws Exception()
        coEvery { pdlOppslagService.hentFnrMedSystemToken("2612733882412") } returns "26127338824"
        coEvery { pdlOppslagService.hentAktiveIdenter("mock-token", "19099531196") } returns listOf(
            "19099531196",
            "26127338824",
        )
        coEvery { pdlOppslagService.hentAktiveIdenter("mock-token", "26127338824") } returns listOf("2612733882412")

        val opprettHendelse = InnkommendeHendelse(
            aktoerer = listOf("1909953119612", "2612733882412"),
            identer = listOf(),
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
            primaerBehandling = SoknadsstatusDomain.PrimaerBehandling("1500oVFWj", "forrige"),
        )

        runCatching { hendelseService.onNewHendelse(innkommendeHendelse = opprettHendelse) }

        val behandlingResult = behandlingService.getAllForIdent("mock-token", "19099531196")
        assertEquals(0, behandlingResult.size)

        val behandlingResult2 = behandlingService.getAllForIdent("mock-token", "26127338824")
        assertEquals(0, behandlingResult2.size)

        val hendelseResult = hendelseService.getAllForIdent("mock-token", "19099531196")
        assertEquals(0, hendelseResult.size)

        val hendelseResult2 = hendelseService.getAllForIdent("mock-token", "26127338824")
        assertEquals(0, hendelseResult2.size)
    }
}
