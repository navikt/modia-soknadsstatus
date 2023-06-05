package no.nav.modia.soknadsstatus

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.personoversikt.common.utils.EnvUtils
import no.nav.personoversikt.common.utils.EnvUtils.getConfig
import no.nav.personoversikt.common.utils.EnvUtils.getRequiredConfig
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.flywaydb.core.Flyway
import javax.sql.DataSource

class DatasourceConfiguration(appMode: AppMode, appName: String, datasourceEnv: DatasourceEnv) {
    val datasource: DataSource by lazy {
        val config = HikariConfig()

        config.jdbcUrl = datasourceEnv.jdbcUrl
        config.minimumIdle = 2
        config.maximumPoolSize = 10
        config.connectionTimeout = 1000
        config.maxLifetime = 30_000

        if (appMode == AppMode.LOCALLY_WITHIN_DOCKER || appMode == AppMode.LOCALLY_WITHIN_IDEA) {
            config.username = getRequiredConfig("JDBC_USERNAME")
            config.password = getRequiredConfig("JDBC_PASSWORD")
            HikariDataSource(config)
        } else {
            HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
                config,
                requireNotNull(datasourceEnv.mountPath),
                "$appName-user"
            )
        }

    }

    fun runFlyway() {
        Flyway
            .configure()
            .dataSource(datasource)
            .load()
            .migrate()
    }
}

data class DatasourceEnv(
    val jdbcUrl: String = getRequiredConfig("DATABASE_JDBC_URL"),
    val mountPath: String? = getConfig("VAULT_MOUNTPATH")
)