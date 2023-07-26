package no.nav.modia.soknadsstatus.ansatt

import no.nav.common.client.axsys.AxsysClient
import no.nav.common.types.identer.NavIdent
import no.nav.modia.soknadsstatus.ansatt.domain.AnsattEnhet
import no.nav.modia.soknadsstatus.azure.MSGraphService
import no.nav.personoversikt.common.logging.TjenestekallLogg

interface AnsattService {
    fun hentEnhetsliste(ident: NavIdent): List<AnsattEnhet>
    fun hentAnsattFagomrader(ident: String, enhet: String): Set<String>
    suspend fun hentVeiledersGeografiskeOgSensitiveRoller(userToken: String, ident: NavIdent): RolleListe
}

class AnsattServiceImpl(
    private val axsys: AxsysClient,
    private val azureADService: MSGraphService,
    private val sensitiveTilgangsRoller: SensitiveTilgangsRoller,
    private val geografiskeTilgangsRoller: GeografiskeTilgangsRoller
) : AnsattService {

    private val sensitiveOgGeografiskeTilgangsRoller: RolleListe
        get() {
            return RolleListe(
                sensitiveTilgangsRoller.kode6,
                sensitiveTilgangsRoller.kode7,
                sensitiveTilgangsRoller.skjermedePersoner
            ).apply {
                addAll(geografiskeTilgangsRoller.regionaleTilgangsRoller)
                addAll(geografiskeTilgangsRoller.nasjonaleTilgangsRoller)
            }
        }

    override fun hentEnhetsliste(ident: NavIdent): List<AnsattEnhet> {
        return (axsys.hentTilganger(ident) ?: emptyList())
            .map { AnsattEnhet(it.enhetId.get(), it.navn) }
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
                TjenestekallLogg.error(
                    "Klarte ikke å hente ansatt fagområder for $ident $enhet",
                    throwable = it,
                    fields = mapOf()
                )
                emptySet()
            }
    }

    override suspend fun hentVeiledersGeografiskeOgSensitiveRoller(
        userToken: String,
        ident: NavIdent,
    ): RolleListe {
        return azureADService.fetchMultipleGroupsIfUserIsMember(userToken, ident, sensitiveOgGeografiskeTilgangsRoller)
    }
}
