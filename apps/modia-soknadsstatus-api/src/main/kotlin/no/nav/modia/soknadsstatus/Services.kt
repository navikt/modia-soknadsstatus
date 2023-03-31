package no.nav.modia.soknadsstatus

import no.nav.modia.soknadsstatus.pdl.PdlOppslagService
import no.nav.modia.soknadsstatus.pdl.PdlOppslagServiceImpl

interface Services {
    val pdl: PdlOppslagService
    val soknadsstatusService: SoknadsstatusService
}

class ServicesImpl(configuration: Configuration) : Services {
    override val pdl: PdlOppslagService = PdlOppslagServiceImpl(configuration.pdlClient)
    override val soknadsstatusService: SoknadsstatusService = SoknadsstatusServiceImpl(pdl, configuration.repository)
}
