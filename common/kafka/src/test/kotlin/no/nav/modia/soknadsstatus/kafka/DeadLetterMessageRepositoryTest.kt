package no.nav.modia.soknadsstatus.kafka

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


private const val KEY = "skip_key"

@Testcontainers
class DeadLetterMessageRepositoryTest {
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
    val repository: DeadLetterMessageRepository by lazy { DeadLetterMessageRepository("dlq_event_skip", dataSource) }

    @BeforeEach
    fun setUp() {
        val resourceDirectory = Paths.get("src", "test", "resources", "db", "migration")
        val location = Location("filesystem:${resourceDirectory.absolutePathString()}")
        Flyway.configure().dataSource(dataSource).locations(location).load().migrate()
    }

    @Test
    fun `skal hente key og sette til skipped om den eksisterer`() {
        val rows = repository.getAndMarkAsSkipped(KEY)
        assertEquals(1, rows.size)
        assertEquals(KEY, rows[0].key)
    }
}