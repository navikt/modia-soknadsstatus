package no.nav.modia.soknadsstatus.infratructure.ping

import no.nav.common.health.selftest.SelfTestCheck

interface Pingable {
    fun ping(): SelfTestCheck
}
