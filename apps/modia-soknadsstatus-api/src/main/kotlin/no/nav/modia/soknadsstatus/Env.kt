package no.nav.modia.soknadsstatus

import no.nav.modia.soknadsstatus.utils.DownstreamApi
import no.nav.modia.soknadsstatus.utils.parse
import no.nav.personoversikt.common.utils.EnvUtils

interface Env {
    companion object {
        operator fun invoke() = EnvImpl()
    }

    val appName: String
    val appVersion: String
    val clusterName: String
    val brokerUrl: String
    val sourceTopic: String
    val azureAppWellKnownUrl: String
    val pdlApiUrl: String
    val pdlScope: DownstreamApi
    val datasourceConfiguration: DatasourceConfiguration
}

data class EnvImpl(
    override val appName: String = EnvUtils.getRequiredConfig("APP_NAME"),
    override val appVersion: String = EnvUtils.getRequiredConfig("APP_VERSION"),
    override val brokerUrl: String = EnvUtils.getRequiredConfig("KAFKA_BROKER_URL"),
    override val sourceTopic: String = EnvUtils.getRequiredConfig("KAFKA_SOURCE_TOPIC"),
    override val azureAppWellKnownUrl: String = EnvUtils.getRequiredConfig("AZURE_APP_WELL_KNOWN_URL"),
    override val clusterName: String = EnvUtils.getRequiredConfig("NAIS_CLUSTER_NAME"),
    override val pdlApiUrl: String = EnvUtils.getRequiredConfig("PDL_API_URL"),
    override val pdlScope: DownstreamApi = EnvUtils.getRequiredConfig("PDL_SCOPE").toDownstreamApi(),
    override val datasourceConfiguration: DatasourceConfiguration = DatasourceConfiguration()
) : Env

private fun String.toDownstreamApi() = DownstreamApi.parse(this)
