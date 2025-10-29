package no.nav.modia.soknadsstatus.axsys

import no.nav.common.client.axsys.AxsysClient
import no.nav.common.client.axsys.AxsysEnhet
import no.nav.common.health.HealthCheckResult
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.NavIdent
import no.nav.modia.soknadsstatus.MockData

class AxsysClientMock : AxsysClient {
    override fun checkHealth(): HealthCheckResult = HealthCheckResult.healthy()

    override fun hentAnsatte(enhetId: EnhetId?): MutableList<NavIdent> = mutableListOf(NavIdent(MockData.Veileder.navIdent))

    override fun hentTilganger(navIdent: NavIdent?): MutableList<AxsysEnhet> {
        val enhet = AxsysEnhet().setEnhetId(MockData.Veileder.axsysEnhet)
        return mutableListOf(enhet)
    }
}
