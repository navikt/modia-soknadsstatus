package no.nav.modia.soknadsstatus.accesscontrol.kabac.providers

import io.ktor.server.auth.*
import no.nav.personoversikt.common.kabac.Kabac
import no.nav.personoversikt.common.kabac.utils.Key
import no.nav.personoversikt.common.ktor.utils.Security

class AuthContextPip(
    private val authContext: AuthenticationContext,
) : Kabac.PolicyInformationPoint<Security.SubjectPrincipal> {
    override val key = Companion.key
    companion object : Kabac.AttributeKey<Security.SubjectPrincipal> {
        override val key = Key<Security.SubjectPrincipal>(AuthContextPip::class.java.simpleName)
    }

    override fun provide(ctx: Kabac.EvaluationContext): Security.SubjectPrincipal =
        requireNotNull(authContext.principal()) {
            "Principal was null"
        }
}
