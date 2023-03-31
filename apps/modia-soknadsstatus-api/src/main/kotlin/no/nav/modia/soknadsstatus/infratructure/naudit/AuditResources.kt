package no.nav.modia.soknadsstatus.infratructure.naudit

import no.nav.modia.soknadsstatus.infratructure.naudit.Audit.AuditResource

class AuditResources {
    class Introspection {
        companion object {
            val Tokens = AuditResource("introspection.token")
            val Pdlsok = AuditResource("introspection.pdlsok")
        }
    }
    class Person {
        class SakOgBehandling {
            companion object {
                val Les = AuditResource("person.sakOgBehandling.les")
            }
        }
    }
}
