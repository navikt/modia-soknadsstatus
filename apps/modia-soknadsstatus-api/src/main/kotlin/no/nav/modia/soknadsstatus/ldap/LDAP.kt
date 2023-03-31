package no.nav.modia.soknadsstatus.ldap

import no.nav.common.types.identer.NavIdent
import javax.naming.ldap.LdapContext

interface LDAPService {
    fun hentRollerForVeileder(ident: NavIdent): List<String>
}

interface LDAPContextProvider {
    fun getContext(): LdapContext
    val baseDN: String
}

object LDAP {
    fun parseADRolle(raw: String): String {
        check(raw.startsWith("CN=")) {
            "Feil format på AD-rolle: $raw"
        }
        return raw.split(",")[0].split("CN=")[1]
    }
}
