package no.nav.modia.soknadsstatus.pdlpip

import no.nav.common.health.HealthCheckResult
import no.nav.common.health.selftest.SelfTestCheck
import no.nav.common.types.identer.Fnr
import no.nav.modia.soknadsstatus.consumer.pdlPipApi.generated.models.PipAdressebeskyttelse
import no.nav.modia.soknadsstatus.consumer.pdlPipApi.generated.models.PipGeografiskTilknytning
import no.nav.modia.soknadsstatus.consumer.pdlPipApi.generated.models.PipPerson
import no.nav.modia.soknadsstatus.consumer.pdlPipApi.generated.models.PipPersondataResponse

class PdlPipApiMock : PdlPipApi {
    override fun hentPdlPipPerson(fnr: Fnr) =
        PipPersondataResponse(
            person = PipPerson(adressebeskyttelse = listOf(PipAdressebeskyttelse(gradering = "ugradert"))),
            geografiskTilknytning = PipGeografiskTilknytning(),
        )

    override fun ping(): SelfTestCheck =
        SelfTestCheck("Mock pdl-pip-api", false) {
            HealthCheckResult.healthy()
        }
}
