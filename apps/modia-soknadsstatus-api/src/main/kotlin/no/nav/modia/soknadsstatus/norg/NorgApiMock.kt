package no.nav.modia.soknadsstatus.norg

import no.nav.common.health.HealthCheckResult
import no.nav.common.health.selftest.SelfTestCheck
import no.nav.common.types.identer.EnhetId
import no.nav.modia.soknadsstatus.MockData

class NorgApiMock : NorgApi {
    override fun finnNavKontor(
        geografiskTilknytning: String,
        diskresjonskode: NorgDomain.DiskresjonsKode?
    ): NorgDomain.Enhet = MockData.veileder.enhet

    override fun hentRegionalEnheter(enhet: List<EnhetId>): List<EnhetId> = listOf(EnhetId(MockData.veileder.enhetId))

    override fun hentRegionalEnhet(enhet: EnhetId): EnhetId = EnhetId(MockData.veileder.enhetId)

    override fun ping(): SelfTestCheck = SelfTestCheck("Mock norgapi", false) {
        HealthCheckResult.healthy()
    }
}