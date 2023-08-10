package no.nav.modia.soknadsstatus.skjermedepersoner

import no.nav.common.health.HealthCheckResult
import no.nav.common.health.selftest.SelfTestCheck
import no.nav.common.types.identer.Fnr

class SkjermedePersonerApiMock : SkjermedePersonerApi {
    override fun erSkjermetPerson(fnr: Fnr): Boolean = false

    override fun ping(): SelfTestCheck = SelfTestCheck("Mock skjermedepersoner", false) {
        HealthCheckResult.healthy()
    }
}
