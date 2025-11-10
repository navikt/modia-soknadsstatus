package no.nav.modia.soknadsstatus.accesscontrol.kabac.policies

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.mockk.every
import io.mockk.mockk
import no.nav.common.types.identer.*
import no.nav.modia.soknadsstatus.accesscontrol.kabac.CommonAttributes
import no.nav.modia.soknadsstatus.accesscontrol.kabac.providers.*
import no.nav.modia.soknadsstatus.tilgangsmaskinen.TilgangsMaskinResponse
import no.nav.modia.soknadsstatus.tilgangsmaskinen.Tilgangsmaskinen
import no.nav.personoversikt.common.kabac.Kabac
import no.nav.personoversikt.common.kabac.KabacTestUtils
import no.nav.personoversikt.common.ktor.utils.Security
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

@TestInstance(Lifecycle.PER_CLASS)
internal class TilgangTilBrukerPolicyTest {
    private val policy =
        KabacTestUtils.PolicyTester(
            TilgangTilBrukerPolicy(),
        )
    private val tilgangsmaskinen = mockk<Tilgangsmaskinen>()

    private val fnr = Fnr("10108000398")
    private val veilederIdent = NavIdent("Z99999")
    private val token = JWT.create().withSubject(veilederIdent.get()).sign(Algorithm.none())

    @Test
    internal fun `permit om veileder har tilgang`() {
        every { tilgangsmaskinen.sjekkTilgang(veilederIdent, fnr) } returns TilgangsMaskinResponse(true)
        policy.assertPermit(*fellesPipTjenester(), CommonAttributes.FNR.withValue(fnr))
    }

    @Test
    internal fun `deny om veileder har ikke tilgang`() {
        every { tilgangsmaskinen.sjekkTilgang(veilederIdent, fnr) } returns TilgangsMaskinResponse(false)
        policy.assertDeny(*fellesPipTjenester(), CommonAttributes.FNR.withValue(fnr))
    }

    @Test
    internal fun `deny om tilgangsmaskinen feiler`() {
        every { tilgangsmaskinen.sjekkTilgang(veilederIdent, fnr) } returns null
        policy.assertDeny(*fellesPipTjenester(), CommonAttributes.FNR.withValue(fnr))
    }

    private fun fellesPipTjenester(): Array<Kabac.PolicyInformationPoint<*>> =
        arrayOf(
            AuthContextPip.key.withValue(Security.SubjectPrincipal(token)),
            NavIdentPip.key.withValue(veilederIdent),
            TilgangsMaskinenPip(tilgangsmaskinen),
        )
}
