package no.nav.modia.soknadsstatus.ldap

import no.nav.common.types.identer.NavIdent
import no.nav.modia.soknadsstatus.MockData

class LDAPServiceMock : LDAPService {
    override fun hentRollerForVeileder(ident: NavIdent) = MockData.veileder.roller
}

