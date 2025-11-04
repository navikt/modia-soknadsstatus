import no.nav.common.health.HealthCheckResult
import no.nav.common.health.selftest.SelfTestCheck
import no.nav.common.types.identer.Fnr
import no.nav.modia.soknadsstatus.tilgangsmaskinen.TilgangsMaskinResponse
import no.nav.modia.soknadsstatus.tilgangsmaskinen.Tilgangsmaskinen

class TilgangsmaskinenMock : Tilgangsmaskinen {
    override fun sjekkTilgang(fnr: Fnr): TilgangsMaskinResponse? {
        TODO("Not yet implemented")
    }

    override fun ping(): SelfTestCheck =
        SelfTestCheck("Mock skjermedepersoner", false) {
            HealthCheckResult.healthy()
        }
}
