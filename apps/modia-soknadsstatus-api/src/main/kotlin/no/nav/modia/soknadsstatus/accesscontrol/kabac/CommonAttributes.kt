package no.nav.modia.soknadsstatus.accesscontrol.kabac

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.personoversikt.common.kabac.utils.Key

object CommonAttributes {
    private val base: String = CommonAttributes::class.java.simpleName
    val FNR = Key<Fnr>("$base.fnr")
    val AKTOR_ID = Key<AktorId>("$base.aktor_id")
}
