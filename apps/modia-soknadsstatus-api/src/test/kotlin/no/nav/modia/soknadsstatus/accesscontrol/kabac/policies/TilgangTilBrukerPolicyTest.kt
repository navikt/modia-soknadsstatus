package no.nav.modia.soknadsstatus.accesscontrol.kabac.policies

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.api.generated.pdl.enums.AdressebeskyttelseGradering
import no.nav.api.generated.pdl.hentadressebeskyttelse.Adressebeskyttelse
import no.nav.common.types.identer.*
import no.nav.modia.soknadsstatus.accesscontrol.kabac.CommonAttributes
import no.nav.modia.soknadsstatus.accesscontrol.kabac.providers.*
import no.nav.modia.soknadsstatus.ansatt.*
import no.nav.modia.soknadsstatus.ansatt.domain.AnsattEnhet
import no.nav.modia.soknadsstatus.kafka.AppCluster
import no.nav.modia.soknadsstatus.norg.NorgApi
import no.nav.modia.soknadsstatus.norg.NorgDomain
import no.nav.modia.soknadsstatus.pdl.PdlOppslagService
import no.nav.modia.soknadsstatus.skjermedepersoner.SkjermedePersonerApi
import no.nav.personoversikt.common.kabac.Kabac
import no.nav.personoversikt.common.kabac.KabacTestUtils
import no.nav.personoversikt.common.ktor.utils.Security
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import java.util.*

@TestInstance(Lifecycle.PER_CLASS)
internal class TilgangTilBrukerPolicyTest {
    private val sensitiveTilgangsRoller = SensitiveTilgangsRoller(appCluster = AppCluster.PROD)
    private val geografiskeTilgangsRoller = GeografiskeTilgangsRoller(appCluster = AppCluster.PROD)
    private val policy = KabacTestUtils.PolicyTester(
        TilgangTilBrukerPolicy(
            sensitiveTilgangsRoller,
            geografiskeTilgangsRoller,
        ),
    )
    private val pdl = mockk<PdlOppslagService>()
    private val norg = mockk<NorgApi>()
    private val skjermedePersoner = mockk<SkjermedePersonerApi>()
    private val ansattService = mockk<AnsattService>()

    private val ident = NavIdent("Z999999")
    private val fnr = Fnr("10108000398")
    private val aktorId = AktorId("987654321987")
    private val token = JWT.create().withSubject(ident.get()).sign(Algorithm.none())

    @Test
    internal fun `permit om veileder har nasjonal tilgang`() {
        gittAtBrukerIkkeHarAdressebeskyttelse()
        gittAtBrukerIkkeErSkjermet()
        gittAtVeilederHarTilgangTilEnhet(EnhetId("0202"))
        gittAtVeilederHarNasjonalTilgang()

        policy.assertPermit(*fellesPipTjenester(), CommonAttributes.FNR.withValue(fnr))
    }

    @Test
    internal fun `permit om bruker (FNR) ikke har nav kontor`() {
        gittAtBrukerIkkeHarAdressebeskyttelse()
        gittAtBrukerIkkeErSkjermet()
        gittAtBrukerHarEnhet(null)
        gittAtVeilederHarTilgangTilEnhet(EnhetId("0202"))

        policy.assertPermit(*fellesPipTjenester(), CommonAttributes.FNR.withValue(fnr))
    }

    @Test
    internal fun `permit om bruker (AKTOR_ID) ikke har nav kontor`() {
        gittFnrAktorIdMapping(fnr to aktorId)
        gittAtBrukerIkkeHarAdressebeskyttelse()
        gittAtBrukerIkkeErSkjermet()
        gittAtBrukerHarEnhet(null)
        gittAtVeilederHarTilgangTilEnhet(EnhetId("0202"))

        policy.assertPermit(*fellesPipTjenester(), CommonAttributes.AKTOR_ID.withValue(aktorId))
    }

    @Test
    internal fun `permit om veileder har tilgang til brukers nav kontor`() {
        gittAtBrukerIkkeHarAdressebeskyttelse()
        gittAtBrukerIkkeErSkjermet()
        gittAtBrukerHarEnhet(EnhetId("0101"))
        gittAtVeilederHarTilgangTilEnhet(EnhetId("0101"))

        policy.assertPermit(*fellesPipTjenester(), CommonAttributes.FNR.withValue(fnr))
    }

    @Test
    internal fun `permit om veileder har regional tilgang til brukers nav region kontor`() {
        gittAtBrukerIkkeHarAdressebeskyttelse()
        gittAtBrukerIkkeErSkjermet()
        gittAtBrukerHarEnhet(EnhetId("0101"))
        gittAtVeilederHarRegionalTilgang()
        gittAtVeilederHarTilgangTilEnhet(EnhetId("0202"))
        gittRegionEnheter(
            EnhetId("0101") to EnhetId("0600"),
            EnhetId("0202") to EnhetId("0600"),
        )

        policy.assertPermit(*fellesPipTjenester(), CommonAttributes.FNR.withValue(fnr))
        gittAtVeilederIkkeHarNoenSpesielleRoller()
        policy
            .assertDeny(*fellesPipTjenester(), CommonAttributes.FNR.withValue(fnr))
            .withMessage("Veileder har ikke tilgang til bruker basert på geografisk tilgang")
    }

    @Test
    internal fun `deny i alle andre tilfeller`() {
        gittAtBrukerIkkeHarAdressebeskyttelse()
        gittAtBrukerIkkeErSkjermet()
        gittAtBrukerHarEnhet(EnhetId("0101"))
        gittAtVeilederHarRegionalTilgang()
        gittAtVeilederHarTilgangTilEnhet(EnhetId("0202"))
        gittRegionEnheter(
            EnhetId("0101") to EnhetId("0600"),
            EnhetId("0202") to EnhetId("0601"),
        )

        policy
            .assertDeny(*fellesPipTjenester(), CommonAttributes.FNR.withValue(fnr))
            .withMessage("Veileder har ikke tilgang til bruker basert på geografisk tilgang")
    }

    @Test
    internal fun `deny om veileder mangler tilgang til skjermet bruker`() {
        gittAtBrukerIkkeHarAdressebeskyttelse()
        gittAtBrukerErSkjermet()
        gittAtBrukerHarEnhet(null)
        gittAtVeilederHarTilgangTilEnhet(EnhetId("0202"))

        policy
            .assertDeny(*fellesPipTjenester(), CommonAttributes.FNR.withValue(fnr))
            .withMessage("Veileder har ikke tilgang til skjermet person")

        gittAtVeilederHarTilgangTilSkjermetPerson()
        policy
            .assertPermit(*fellesPipTjenester(), CommonAttributes.FNR.withValue(fnr))
    }

    @Test
    internal fun `deny om veileder mangler tilgang til bruker med kode6`() {
        gittAtBrukerHarKode6()
        gittAtBrukerIkkeErSkjermet()
        gittAtBrukerHarEnhet(null)
        gittAtVeilederHarTilgangTilEnhet(EnhetId("0202"))

        policy
            .assertDeny(*fellesPipTjenester(), CommonAttributes.FNR.withValue(fnr))
            .withMessage("Veileder har ikke tilgang til kode6")

        gittAtVeilederHarTilgangTilKode6()
        policy
            .assertPermit(*fellesPipTjenester(), CommonAttributes.FNR.withValue(fnr))
    }

    @Test
    internal fun `deny om veileder mangler tilgang til bruker med kode7`() {
        gittAtBrukerHarKode7()
        gittAtBrukerIkkeErSkjermet()
        gittAtBrukerHarEnhet(null)
        gittAtVeilederHarTilgangTilEnhet(EnhetId("0202"))

        policy
            .assertDeny(*fellesPipTjenester(), CommonAttributes.FNR.withValue(fnr))
            .withMessage("Veileder har ikke tilgang til kode7")

        gittAtVeilederHarTilgangTilKode7()
        policy
            .assertPermit(*fellesPipTjenester(), CommonAttributes.FNR.withValue(fnr))
    }

    private fun gittAtBrukerIkkeHarAdressebeskyttelse() {
        coEvery { pdl.hentAdresseBeskyttelse(any(), fnr.get()) } returns listOf(
            Adressebeskyttelse(
                gradering = AdressebeskyttelseGradering.UGRADERT,
            ),
        )
    }

    private fun gittAtBrukerIkkeErSkjermet() {
        every { skjermedePersoner.erSkjermetPerson(fnr) } returns false
    }

    private fun gittAtBrukerErSkjermet() {
        every { skjermedePersoner.erSkjermetPerson(fnr) } returns true
    }

    private fun gittAtBrukerHarKode6() {
        coEvery { pdl.hentAdresseBeskyttelse(any(), fnr.get()) } returns listOf(
            Adressebeskyttelse(
                AdressebeskyttelseGradering.STRENGT_FORTROLIG,
            ),
        )
    }

    private fun gittAtBrukerHarKode7() {
        coEvery { pdl.hentAdresseBeskyttelse(any(), fnr.get()) } returns listOf(
            Adressebeskyttelse(
                AdressebeskyttelseGradering.FORTROLIG,
            ),
        )
    }

    private fun gittAtBrukerHarEnhet(enhetId: EnhetId?) {
        val geografiskTilknyttning = UUID.randomUUID().toString()
        coEvery { pdl.hentGeografiskTilknytning(any(), fnr.get()) } returns geografiskTilknyttning

        if (enhetId == null) {
            every { norg.finnNavKontor(geografiskTilknyttning, null) } returns null
        } else {
            every { norg.finnNavKontor(geografiskTilknyttning, null) } returns NorgDomain.Enhet(
                enhetId = enhetId.get(),
                enhetNavn = "Navn",
                status = NorgDomain.EnhetStatus.AKTIV,
                oppgavebehandler = false,
            )
        }
    }

    private fun gittAtVeilederHarRoller(roller: RolleListe) {
        coEvery { ansattService.hentVeiledersGeografiskeOgSensitiveRoller(token, ident) } returns roller
    }

    private fun gittAtVeilederHarNasjonalTilgang() {
        return gittAtVeilederHarRoller(RolleListe(geografiskeTilgangsRoller.nasjonaleTilgangsRoller))
    }

    private fun gittAtVeilederHarRegionalTilgang() {
        return gittAtVeilederHarRoller(RolleListe(geografiskeTilgangsRoller.regionaleTilgangsRoller))
    }

    private fun gittAtVeilederHarTilgangTilKode6() {
        return gittAtVeilederHarRoller(RolleListe(sensitiveTilgangsRoller.kode6))
    }

    private fun gittAtVeilederHarTilgangTilKode7() {
        return gittAtVeilederHarRoller(RolleListe(sensitiveTilgangsRoller.kode7))
    }

    private fun gittAtVeilederHarTilgangTilSkjermetPerson() {
        return gittAtVeilederHarRoller(RolleListe(sensitiveTilgangsRoller.skjermedePersoner))
    }

    private fun gittAtVeilederHarTilgangTilEnhet(enhetId: EnhetId) {
        every { ansattService.hentEnhetsliste(ident) } returns listOf(AnsattEnhet(enhetId.get(), "navn"))
    }

    private fun gittAtVeilederIkkeHarNoenSpesielleRoller() {
        return gittAtVeilederHarRoller(RolleListe(AnsattRolle("test", AzureObjectId("test"))))
    }

    private fun gittFnrAktorIdMapping(vararg fnraktoridMapping: Pair<Fnr, AktorId>) {
        val fnrmap = fnraktoridMapping.toMap()
        val aktormap = fnraktoridMapping.associate { Pair(it.second, it.first) }
        coEvery { pdl.hentFnr(any(), any()) } answers {
            aktormap[AktorId(arg<String>(1))]?.get()
        }
        coEvery { pdl.hentAktorId(any(), any()) } answers {
            fnrmap[Fnr(arg<String>(1))]?.get()
        }
    }

    private fun gittRegionEnheter(vararg regionEnhetMapping: Pair<EnhetId, EnhetId>) {
        val regionmap = regionEnhetMapping.toMap()
        every { norg.hentRegionalEnhet(any()) } answers {
            val enhetId = arg<EnhetId>(0)
            regionmap[enhetId]
        }
        every { norg.hentRegionalEnheter(any()) } answers {
            val enhetIder = arg<List<EnhetId>>(0)
            enhetIder.mapNotNull { regionmap[it] }
        }
    }

    private fun fellesPipTjenester(): Array<Kabac.PolicyInformationPoint<*>> {
        return arrayOf(
            AuthContextPip.key.withValue(Security.SubjectPrincipal(token)),
            BrukersFnrPip.key.withValue(fnr),
            NavIdentPip.key.withValue(ident),
            BrukersAktorIdPip(pdl),
            BrukersGeografiskeTilknyttningPip(pdl),
            BrukersEnhetPip(norg),
            BrukersDiskresjonskodePip(pdl),
            BrukersSkjermingPip(skjermedePersoner),
            BrukersRegionEnhetPip(norg),
            VeiledersRollerPip(ansattService),
            VeiledersEnheterPip(ansattService),
            VeiledersRegionEnheterPip(norg),
        )
    }
}
