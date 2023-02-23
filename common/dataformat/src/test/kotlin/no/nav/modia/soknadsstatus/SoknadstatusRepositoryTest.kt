package no.nav.modia.soknadsstatus

import kotlinx.datetime.Clock
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import javax.sql.DataSource
import kotlin.time.Duration.Companion.minutes

@Testcontainers
class soknadsstatusRepositoryTest {
    @Container
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
    fun `should insert new change record`() {
        repository.upsert(dummyOppdatering)

        assertSuccess(repository.get(ident)) { oppdateringer ->
            assertEquals(1, oppdateringer.size)
        }
    }

    @Test
    fun `should update existing record if newer`() {
        repository.upsert(dummyOppdatering)
        repository.upsert(
            dummyOppdatering.copy(
                status = soknadsstatusDomain.Status.FERDIG_BEHANDLET,
                tidspunkt = Clock.System.now()
            )
        )

        assertSuccess(repository.get(ident)) { oppdateringer ->
            assertEquals(1, oppdateringer.size)
            assertEquals(soknadsstatusDomain.Status.FERDIG_BEHANDLET, oppdateringer.first().status)
        }
    }

    @Test
    fun `should ignore updates older then current saved state`() {
        repository.upsert(dummyOppdatering)
        repository.upsert(
            dummyOppdatering.copy(
                status = soknadsstatusDomain.Status.FERDIG_BEHANDLET,
                tidspunkt = Clock.System.now().minus(1.minutes)
            )
        )

        assertSuccess(repository.get(ident)) { oppdateringer ->
            assertEquals(1, oppdateringer.size)
            assertEquals(soknadsstatusDomain.Status.UNDER_BEHANDLING, oppdateringer.first().status)
        }
    }

    val ident = "12345678910"
    val dummyOppdatering = soknadsstatusDomain.soknadsstatusOppdatering(
        ident = ident,
        behandlingsRef = "ABBA1231",
        systemRef = "infotrygd",
        tema = "DAG",
        status = soknadsstatusDomain.Status.UNDER_BEHANDLING,
        tidspunkt = Clock.System.now()
    )

    fun <T> assertSuccess(result: Result<T>, block: (T) -> Unit) {
        assertEquals(true, result.isSuccess)
        block(result.getOrThrow())
    }
}
