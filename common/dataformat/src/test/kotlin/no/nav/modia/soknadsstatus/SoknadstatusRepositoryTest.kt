package no.nav.modia.soknadsstatus

import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class SoknadstatusRepositoryTest {
 /*   @Container
    val container = PostgreSQLContainer("postgres:14-alpine")

    val dataSource: DataSource by lazy {
        PGSimpleDataSource().apply {
            setProperty("PGPORT", container.getMappedPort(5432).toString())
            setProperty("PGDBNAME", "test")
            setProperty("user", "test")
            setProperty("password", "test")
        }
    }
    val repository: soknadsstatusRepository by lazy { soknadsstatusRepository(dataSource) }

    @BeforeEach
    fun setUp() {
        Flyway
            .configure()
            .dataSource(dataSource)
            .load()
            .migrate()
    }

    @Test
    @Ignore
    fun `should insert new change record`() {
        repository.upsert(dummyOppdatering)

        assertSuccess(repository.get(ident)) { oppdateringer ->
            assertEquals(1, oppdateringer.size)
        }
    }

    @Test
    @Ignore
    fun `should update existing record if newer`() {
        repository.upsert(dummyOppdatering)
        repository.upsert(
            dummyOppdatering.copy(
                status = SoknadsstatusDomain.Status.FERDIG_BEHANDLET,
                tidspunkt = Clock.System.now()
            )
        )

        assertSuccess(repository.get(ident)) { oppdateringer ->
            assertEquals(1, oppdateringer.size)
            assertEquals(SoknadsstatusDomain.Status.FERDIG_BEHANDLET, oppdateringer.first().status)
        }
    }

    @Test
    @Ignore
    fun `should ignore updates older then current saved state`() {
        repository.upsert(dummyOppdatering)
        repository.upsert(
            dummyOppdatering.copy(
                status = SoknadsstatusDomain.Status.FERDIG_BEHANDLET,
                tidspunkt = Clock.System.now().minus(1.minutes)
            )
        )

        assertSuccess(repository.get(ident)) { oppdateringer ->
            assertEquals(1, oppdateringer.size)
            assertEquals(SoknadsstatusDomain.Status.UNDER_BEHANDLING, oppdateringer.first().status)
        }
    }

    val ident = "12345678910"
    val dummyOppdatering = SoknadsstatusDomain.SoknadsstatusOppdatering(
        ident = ident,
        behandlingsId = "ABBA1231",
        systemRef = "infotrygd",
        tema = "DAG",
        status = SoknadsstatusDomain.Status.UNDER_BEHANDLING,
        tidspunkt = Clock.System.now()
    )

    fun <T> assertSuccess(result: Result<T>, block: (T) -> Unit) {
        assertEquals(true, result.isSuccess)
        block(result.getOrThrow())
    }
  */
}