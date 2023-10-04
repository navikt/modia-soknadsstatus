package no.nav.modia.soknadsstatus.ansatt

import no.nav.modia.soknadsstatus.kafka.AppCluster
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

private const val KODE6_PROD = "ad7b87a6-9180-467c-affc-20a566b0fec0"
private const val KODE6_PREPROD = "5ef775f2-61f8-4283-bf3d-8d03f428aa14"
private const val KODE7_PROD = "9ec6487d-f37a-4aad-a027-cd221c1ac32b"
private const val KODE7_PREPROD = "ea930b6b-9397-44d9-b9e6-f4cf527a632a"
private const val SKJERMEDE_PERSONER_PROD = "e750ceb5-b70b-4d94-b4fa-9d22467b786b"
private const val SKJERMEDE_PERSONER_PREPROD = "dbe4ad45-320b-4e9a-aaa1-73cca4ee124d"
private val NASJONALE_TILGANGER_PROD =
    listOf(
        "3063daf5-24c6-409c-9139-2d843cb4327b",
        "ad3ee064-44b7-4c58-a618-5df1e1cb05db",
        "f8ceac75-30fe-4f0f-aec6-db706986caf0",
        "baf0a4b6-71ca-45dd-9731-9f3469627c38",
    )
private val NASJONALE_TILGANGER_PREPROD =
    listOf(
        "924badcd-b936-44f4-b7bf-97c03de0893a",
        "2ffe3262-6c0e-44f3-94f0-34dfa5659a04",
        "ea7411eb-8b48-41a0-bc56-7b521fbf0c25",
        "753805ea-65a7-4855-bdc3-e6130348df9f",
    )
private val REGIONALE_TILGANGER_PROD =
    listOf("422852aa-aad5-4601-a8c8-917ef42b6601", "14caf09e-dd9a-43fe-b25e-7f58dd9fdcae")
private val REGIONALE_TILGANGER_PREPROD =
    listOf("d2987104-63b2-4110-83ac-20ff6afe24a2", "a5c2370e-6b3d-4c2c-9a5e-238008526574")

@TestInstance(Lifecycle.PER_CLASS)
class TilgangsRollerTest {
    lateinit var sensitiveTilgangsRoller: SensitiveTilgangsRoller
    lateinit var geografiskeTilgangsRoller: GeografiskeTilgangsRoller

    private fun setupProdCluster() {
        setupCluster("prod-gcp")
    }

    private fun setupPreProdCluster() {
        setupCluster("dev-gcp")
    }

    private fun setupNoCluster() {
        setupCluster(null)
    }

    private fun setupCluster(clusterName: String?) {
        sensitiveTilgangsRoller = SensitiveTilgangsRoller(AppCluster(clusterName))
        geografiskeTilgangsRoller = GeografiskeTilgangsRoller(AppCluster(clusterName))
    }

    @Test
    fun `skal sette opp produksjonsroller når klusteret er i prod`() {
        setupProdCluster()
        assertEquals(sensitiveTilgangsRoller.kode6.gruppeId.get(), KODE6_PROD, "Id for kode6 gruppe i prod var ikke riktig.")
        assertEquals(sensitiveTilgangsRoller.kode7.gruppeId.get(), KODE7_PROD, "Id for kode7 gruppe i prod var ikke riktig.")
        assertEquals(
            sensitiveTilgangsRoller.skjermedePersoner.gruppeId.get(),
            SKJERMEDE_PERSONER_PROD,
            "Id for kode7 gruppe i prod var ikke riktig.",
        )
        assertTrue(
            NASJONALE_TILGANGER_PROD ==
                geografiskeTilgangsRoller.nasjonaleTilgangsRoller.map {
                    it.gruppeId.get()
                },
            "Nasjonale tilganger i prod var ikke riktig.",
        )
        assertTrue(
            REGIONALE_TILGANGER_PROD ==
                geografiskeTilgangsRoller.regionaleTilgangsRoller.map {
                    it.gruppeId.get()
                },
            "Regionale tilganger i prod var ikke riktig.",
        )
    }

    @Test
    fun `skal sette opp preprod roller når klusteret er i dev`() {
        setupPreProdCluster()
        assertEquals(sensitiveTilgangsRoller.kode6.gruppeId.get(), KODE6_PREPROD, "Id for kode6 gruppe i preprod var ikke riktig.")
        assertEquals(sensitiveTilgangsRoller.kode7.gruppeId.get(), KODE7_PREPROD, "Id for kode7 gruppe i preprod var ikke riktig.")
        assertEquals(
            sensitiveTilgangsRoller.skjermedePersoner.gruppeId.get(),
            SKJERMEDE_PERSONER_PREPROD,
            "Id for skjermede personer gruppe i preprod var ikke riktig.",
        )
        assertTrue(
            NASJONALE_TILGANGER_PREPROD ==
                geografiskeTilgangsRoller.nasjonaleTilgangsRoller.map {
                    it.gruppeId.get()
                },
            "Nasjonale tilganger i preprod var ikke riktig.",
        )
        assertTrue(
            REGIONALE_TILGANGER_PREPROD ==
                geografiskeTilgangsRoller.regionaleTilgangsRoller.map {
                    it.gruppeId.get()
                },
            "Regionale tilganger i preprod var ikke riktig.",
        )
    }

    @Test
    fun `skal sette opp prod roller når kluster ikke er satt`() {
        setupNoCluster()
        assertEquals(sensitiveTilgangsRoller.kode6.gruppeId.get(), KODE6_PROD, "Id for kode6 gruppe i prod var ikke riktig.")
        assertEquals(sensitiveTilgangsRoller.kode7.gruppeId.get(), KODE7_PROD, "Id for kode7 gruppe i prod var ikke riktig.")
        assertEquals(
            sensitiveTilgangsRoller.skjermedePersoner.gruppeId.get(),
            SKJERMEDE_PERSONER_PROD,
            "Id for skjermede personer gruppe i prod var ikke riktig.",
        )
        assertTrue(
            NASJONALE_TILGANGER_PROD ==
                geografiskeTilgangsRoller.nasjonaleTilgangsRoller.map {
                    it.gruppeId.get()
                },
            "Nasjonale tilganger i prod var ikke riktig.",
        )
        assertTrue(
            REGIONALE_TILGANGER_PROD ==
                geografiskeTilgangsRoller.regionaleTilgangsRoller.map {
                    it.gruppeId.get()
                },
            "Regionale tilganger i prod var ikke riktig.",
        )
    }
}
