package no.nav.modia.soknadsstatus.accesscontrol.kabac.providers

import no.nav.modia.soknadsstatus.accesscontrol.kabac.CommonAttributes
import no.nav.modia.soknadsstatus.skjermedepersoner.SkjermedePersonerApi
import no.nav.personoversikt.common.kabac.Kabac
import no.nav.personoversikt.common.kabac.Kabac.EvaluationContext
import no.nav.personoversikt.common.kabac.utils.Key

class BrukersSkjermingPip(
    private val skjermedePersonerApi: SkjermedePersonerApi,
) : Kabac.PolicyInformationPoint<Boolean> {
    override val key = Companion.key

    companion object : Kabac.AttributeKey<Boolean> {
        override val key = Key<Boolean>(BrukersSkjermingPip::class.java.simpleName)
    }

    override fun provide(ctx: EvaluationContext): Boolean {
        val fnr = ctx.getValue(CommonAttributes.FNR)
        return skjermedePersonerApi.erSkjermetPerson(fnr)
    }
}
