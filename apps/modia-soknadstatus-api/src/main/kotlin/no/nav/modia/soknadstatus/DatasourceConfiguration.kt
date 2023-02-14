package no.nav.modia.soknadstatus

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.personoversikt.common.utils.EnvUtils.getRequiredConfig
import org.flywaydb.core.Flyway
import javax.sql.DataSource

class DatasourceConfiguration {
    val datasource: DataSource by lazy {
        val config = HikariConfig()

        config.jdbcUrl = getRequiredConfig("JDBC_URL")
        config.minimumIdle = 2
        config.maximumPoolSize = 10
        config.connectionTimeout = 1000
        config.maxLifetime = 30_000
        config.username = getRequiredConfig("JDBC_USERNAME")
        config.password = getRequiredConfig("JDBC_PASSWORD")

        HikariDataSource(config)
    }

    fun runFlyway() {
        Flyway
            .configure()
            .dataSource(datasource)
            .load()
            .migrate()
    }
}
