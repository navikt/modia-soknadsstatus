package no.nav.modia.soknadsstatus

import io.mockk.mockk
import no.nav.modia.soknadsstatus.pdl.PdlOppslagService
import no.nav.modia.soknadsstatus.repository.*
import no.nav.modia.soknadsstatus.service.*
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
    val pdlOppslagService = mockk<PdlOppslagService>()
    lateinit var hendelseRepository: HendelseRepository
    lateinit var hendelseService: HendelseService
    lateinit var behandlingService: BehandlingService

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
        hendelseRepository = HendelseRepositoryImpl(dataSource)
        val behandlingEiereRepository = BehandlingEierRepositoryImpl(dataSource)
        val behandlingEierService = BehandlingEierServiceImpl(behandlingEiereRepository)
        val hendelseEierRepository = HendelseEierRepositoryImpl(dataSource)
        val hendelseEierService = HendelseEierServiceImpl(hendelseEierRepository)
        val behanRepository = BehandlingRepositoryImpl(dataSource)
        behandlingService = BehandlingServiceImpl(behanRepository, pdlOppslagService)
        hendelseService =
            HendelseServiceImpl(
                pdlOppslagService,
                hendelseRepository,
                behandlingEierService,
                hendelseEierService,
            )

        hendelseService.init(behandlingService)
        behandlingService.init(hendelseService)
        val resourceDirectory = Paths.get("src", "main", "resources", "db", "migration")
        val location = Location("filesystem:${resourceDirectory.absolutePathString()}")
        Flyway
            .configure()
            .dataSource(dataSource)
            .locations(location)
            .load()
            .migrate()
    }
}
