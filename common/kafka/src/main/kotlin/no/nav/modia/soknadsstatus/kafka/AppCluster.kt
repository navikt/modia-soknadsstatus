package no.nav.modia.soknadsstatus.kafka

enum class AppCluster {
    PROD,
    PREPROD,
    LOCALLY;

    companion object {
        operator fun invoke(cluster: String?): AppCluster {
            return when (cluster) {
                null -> PROD
                "prod-gcp" -> PROD
                "dev-gcp" -> PREPROD
                "locally" -> LOCALLY
                else -> PROD
            }
        }
    }
}
