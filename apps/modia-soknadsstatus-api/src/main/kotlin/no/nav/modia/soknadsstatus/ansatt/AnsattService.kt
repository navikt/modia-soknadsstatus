package no.nav.modia.soknadsstatus.ansatt

import no.nav.common.client.axsys.AxsysClient
import no.nav.common.client.nom.NomClient
import no.nav.common.types.identer.NavIdent
import no.nav.modia.soknadsstatus.accesscontrol.kabac.RolleListe
import no.nav.modia.soknadsstatus.ansatt.domain.AnsattEnhet
import no.nav.modia.soknadsstatus.ldap.LDAPService
import no.nav.personoversikt.common.logging.Logging.secureLog

interface AnsattService {
    fun hentEnhetsliste(ident: NavIdent): List<AnsattEnhet>
    fun hentVeilederRoller(ident: NavIdent): RolleListe
    fun hentAnsattFagomrader(ident: String, enhet: String): Set<String>
}

class AnsattServiceImpl(
    private val axsys: AxsysClient,
    private val nomClient: NomClient,
    private val ldap: LDAPService,
) : AnsattService {
    override fun hentEnhetsliste(ident: NavIdent): List<AnsattEnhet> {
        return (axsys.hentTilganger(ident) ?: emptyList())
            .map { AnsattEnhet(it.enhetId.get(), it.navn) }
    }

    override fun hentVeilederRoller(ident: NavIdent): RolleListe {
        return RolleListe(ldap.hentRollerForVeileder(ident))
    }

    override fun hentAnsattFagomrader(ident: String, enhet: String): Set<String> {
        return axsys
            .runCatching {
                hentTilganger(NavIdent(ident))
                    .find {
                        it.enhetId.get() == enhet
                    }
                    ?.temaer
                    ?.toSet()
                    ?: emptySet()
            }
            .getOrElse {
                secureLog.error("Klarte ikke å hente ansatt fagområder for $ident $enhet", it)
                emptySet()
            }
    }
}
