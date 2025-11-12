import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.modia.soknadsstatus.tilgangsmaskinen.TilgangsMaskinResponse
import no.nav.modia.soknadsstatus.tilgangsmaskinen.Tilgangsmaskinen

class TilgangsmaskinenMock : Tilgangsmaskinen {
    override fun sjekkTilgang(
        veilederIdent: NavIdent,
        fnr: Fnr,
    ): TilgangsMaskinResponse = TilgangsMaskinResponse(harTilgang = true)
}
