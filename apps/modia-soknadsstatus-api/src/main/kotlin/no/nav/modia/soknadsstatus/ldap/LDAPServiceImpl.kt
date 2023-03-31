package no.nav.modia.soknadsstatus.ldap

import no.nav.common.types.identer.NavIdent
import no.nav.modia.soknadsstatus.MockData
import no.nav.modia.soknadsstatus.ldap.LDAP.parseADRolle
import org.slf4j.LoggerFactory
import javax.naming.directory.SearchControls
import javax.naming.directory.SearchResult

class LDAPServiceImpl(private val contextProvider: LDAPContextProvider) : LDAPService {
    private val searchBase = "OU=Users,OU=NAV,OU=BusinessUnits,${contextProvider.baseDN}"
    private val log = LoggerFactory.getLogger(LDAPServiceImpl::class.java)

    override fun hentRollerForVeileder(ident: NavIdent): List<String> {
        val result = search(ident).firstOrNull() ?: return emptyList()
        val memberof = result.attributes.get("memberof").all.toList()
        return memberof
            .filterIsInstance(String::class.java)
            .map(::parseADRolle)
    }

    private fun search(ident: NavIdent): Sequence<SearchResult> {
        val searchCtl = SearchControls().apply {
            searchScope = SearchControls.SUBTREE_SCOPE
        }
        return contextProvider
            .getContext()
            .search(
                searchBase,
                "(&(objectClass=user)(CN=${ident.get()}))",
                searchCtl
            ).asSequence()
    }
}
