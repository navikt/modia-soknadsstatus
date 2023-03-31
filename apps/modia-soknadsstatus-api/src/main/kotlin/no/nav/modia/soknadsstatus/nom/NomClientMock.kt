package no.nav.modia.soknadsstatus.nom

import no.nav.common.client.nom.NomClient
import no.nav.common.client.nom.VeilederNavn
import no.nav.common.health.HealthCheckResult
import no.nav.common.types.identer.NavIdent

class NomClientMock : NomClient {
    override fun checkHealth(): HealthCheckResult = HealthCheckResult.healthy()

    override fun finnNavn(navIdent: NavIdent): VeilederNavn {
        return lagVeilederNavn(navIdent)
    }

    override fun finnNavn(identer: MutableList<NavIdent>): List<VeilederNavn> {
        return identer.map(::lagVeilederNavn)
    }

    private fun lagVeilederNavn(navIdent: NavIdent): VeilederNavn {
        val ident = navIdent.get()
        val identNr = ident.substring(1)
        return VeilederNavn()
            .setNavIdent(navIdent)
            .setFornavn("F_$identNr")
            .setEtternavn("E_$identNr")
            .setVisningsNavn("F_$identNr E_$identNr")
    }
}