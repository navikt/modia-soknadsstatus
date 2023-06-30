package no.nav.modia.soknadsstatus

import kotlinx.datetime.Clock
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.Location
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.file.Paths
import javax.sql.DataSource
import kotlin.io.path.absolutePathString
import kotlin.time.Duration.Companion.minutes

@Testcontainers
class SoknadsstatusRepositoryImplTest {
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
    val repository: SoknadsstatusRepositoryImpl by lazy { SoknadsstatusRepositoryImpl(dataSource) }

    @BeforeEach
    fun setUp() {
        val resourceDirectory = Paths.get("src", "test", "resources", "db", "migration")
        val location = Location("filesystem:${resourceDirectory.absolutePathString()}")
        Flyway.configure().dataSource(dataSource).locations(location).load().migrate()
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
}
