package no.nav.modia.soknadstatus

import no.nav.modia.soknadstatus.pdl.PdlOppslagService
import no.nav.modia.soknadstatus.pdl.PdlOppslagServiceImpl
import no.nav.modia.soknadstatus.pdl.PdlOppslagServiceTestImpl
import no.nav.personoversikt.common.utils.EnvUtils

data class Configuration(
    val appname: String = EnvUtils.getRequiredConfig("APP_NAME"),
    val appversion: String = EnvUtils.getRequiredConfig("APP_VERSION"),
    val brokerUrl: String = EnvUtils.getRequiredConfig("KAFKA_BROKER_URL"),
    val sourceTopic: String = EnvUtils.getRequiredConfig("KAFKA_SOURCE_TOPIC"),
    val pdlOppslagService: PdlOppslagService = createPdlOppslagService()
)

private fun createPdlOppslagService(): PdlOppslagService {
    val pdlApiUrl = EnvUtils.getConfig("PDL_API_URL")
    return if (pdlApiUrl != null) {
        PdlOppslagServiceImpl()
    } else {
        PdlOppslagServiceTestImpl()
    }
}
