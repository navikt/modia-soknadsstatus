package no.nav.modia.soknadsstatus.ldap

import java.util.*
import javax.naming.Context
import javax.naming.ldap.InitialLdapContext
import javax.naming.ldap.LdapContext

class LDAPContextProviderImpl(env: LDAPEnv) : LDAPContextProvider {
    override val baseDN = env.baseDN
    private val ldapEnvironment = Hashtable(
        mutableMapOf(
            Context.INITIAL_CONTEXT_FACTORY to "com.sun.jndi.ldap.LdapCtxFactory",
            Context.SECURITY_AUTHENTICATION to "simple",
            Context.PROVIDER_URL to env.url,
            Context.SECURITY_PRINCIPAL to env.username,
            Context.SECURITY_CREDENTIALS to env.password,
        )
    )
    override fun getContext(): LdapContext {
        return InitialLdapContext(ldapEnvironment, null)
    }
}

data class LDAPEnv(
    val url: String,
    val username: String,
    val password: String,
    val baseDN: String,
)
