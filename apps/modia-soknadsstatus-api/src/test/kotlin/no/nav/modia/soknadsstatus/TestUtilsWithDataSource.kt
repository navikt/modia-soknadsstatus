package no.nav.modia.soknadsstatus

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.Location
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.file.Paths
import javax.sql.DataSource
import kotlin.io.path.absolutePathString

@Testcontainers
open class TestUtilsWithDataSource {
    @Container
    protected val container = PostgreSQLContainer("postgres:14-alpine")

    protected val dataSource: DataSource by lazy {
        PGSimpleDataSource().apply {
            setProperty("PGPORT", container.getMappedPort(5432).toString())
            setProperty("PGDBNAME", "test")
            setProperty("user", "test")
            setProperty("password", "test")
        }
    }

    open fun setUp() {
        val resourceDirectory = Paths.get("src", "test", "resources", "db", "migration")
        val location = Location("filesystem:${resourceDirectory.absolutePathString()}")
        Flyway.configure().dataSource(dataSource).locations(location).load().migrate()
    }
}
