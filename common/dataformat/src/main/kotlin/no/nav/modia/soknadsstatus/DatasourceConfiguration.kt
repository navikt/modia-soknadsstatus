package no.nav.modia.soknadsstatus

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.personoversikt.common.utils.EnvUtils.getRequiredConfig
import org.flywaydb.core.Flyway
import javax.sql.DataSource

class DatasourceConfiguration(datasourceEnv: DatasourceEnv) {
    val datasource: DataSource by lazy {
        val config = HikariConfig()

        config.jdbcUrl = datasourceEnv.jdbcUrl
        config.minimumIdle = 2
        config.maximumPoolSize = 10
        config.connectionTimeout = 1000
        config.maxLifetime = 30_000
        config.username = datasourceEnv.userName
        config.password = datasourceEnv.password

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

class DatasourceEnv(appName: String, appDB: String = getRequiredConfig("DB_NAME")) {
    private val appDbString = "NAIS_DATABASE_${appName}_${appDB}"

    private val host = getEnvVariable("HOST")
    private val port = getEnvVariable("PORT").toInt()
    val jdbcUrl = "jdbc:postgresql://$host:$port/$appDB"
    val userName = getEnvVariable("USERNAME")
    val password = getEnvVariable("PASSWORD")
    private fun getEnvVariable(suffix: String): String {
        val completeString = "${appDbString}_$suffix"
        return getRequiredConfig(completeString)
    }
}