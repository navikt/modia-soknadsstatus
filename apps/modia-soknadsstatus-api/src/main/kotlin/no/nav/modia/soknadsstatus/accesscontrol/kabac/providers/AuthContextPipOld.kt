package no.nav.modia.soknadsstatus.accesscontrol.kabac.providers

import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.personoversikt.common.kabac.Kabac
import no.nav.personoversikt.common.kabac.utils.Key

object AuthContextPipOld : Kabac.PolicyInformationPoint<AuthContextHolder> {
    private val contextHolder = AuthContextHolderThreadLocal.instance()

    override val key = Key<AuthContextHolder>(AuthContextPipOld)
    override fun provide(ctx: Kabac.EvaluationContext): AuthContextHolder = contextHolder
}
