package no.nav.modia.soknadsstatus.ansatt

import no.nav.common.types.identer.AzureObjectId
import no.nav.modia.soknadsstatus.kafka.AppCluster

private val NASJONAL_ROLLER =
    listOf(
        GeografiskTilgangsRolle(
            groupName = "0000-ga-gosys_nasjonal",
            productionId = "3063daf5-24c6-409c-9139-2d843cb4327b",
            preprodId = "924badcd-b936-44f4-b7bf-97c03de0893a",
        ),
        GeografiskTilgangsRolle(
            groupName = "0000-ga-gosys_utvidbar_til_nasjonal",
            productionId = "ad3ee064-44b7-4c58-a618-5df1e1cb05db",
            preprodId = "2ffe3262-6c0e-44f3-94f0-34dfa5659a04",
        ),
        GeografiskTilgangsRolle(
            groupName = "0000-ga-pensjon_nasjonal_u_logg",
            productionId = "f8ceac75-30fe-4f0f-aec6-db706986caf0",
            preprodId = "ea7411eb-8b48-41a0-bc56-7b521fbf0c25",
        ),
        GeografiskTilgangsRolle(
            groupName = "0000-ga-pensjon_nasjonal_m_logg",
            productionId = "baf0a4b6-71ca-45dd-9731-9f3469627c38",
            preprodId = "753805ea-65a7-4855-bdc3-e6130348df9f",
        ),
    )

private val REGIONALE_ROLLER =
    listOf(
        GeografiskTilgangsRolle(
            groupName = "0000-ga-gosys_regional",
            productionId = "422852aa-aad5-4601-a8c8-917ef42b6601",
            preprodId = "d2987104-63b2-4110-83ac-20ff6afe24a2",
        ),
        GeografiskTilgangsRolle(
            groupName = "0000-ga-gosys_utvidbar_til_regional",
            productionId = "14caf09e-dd9a-43fe-b25e-7f58dd9fdcae",
            preprodId = "a5c2370e-6b3d-4c2c-9a5e-238008526574",
        ),
    )

private data class GeografiskTilgangsRolle(
    val groupName: String,
    val productionId: String,
    val preprodId: String,
)

class GeografiskeTilgangsRoller(
    private val appCluster: AppCluster,
) {
    val nasjonaleTilgangsRoller = getRolesForCluster(NASJONAL_ROLLER)
    val regionaleTilgangsRoller = getRolesForCluster(REGIONALE_ROLLER)

    private fun getRolesForCluster(roles: List<GeografiskTilgangsRolle>): RolleListe =
        RolleListe(
            roles.map {
                AnsattRolle(gruppeNavn = it.groupName, gruppeId = getIdForCluster(it))
            },
        )

    private fun getIdForCluster(rolle: GeografiskTilgangsRolle): AzureObjectId {
        val stringId =
            when (appCluster) {
                AppCluster.PROD -> rolle.productionId
                AppCluster.PREPROD -> rolle.preprodId
                AppCluster.LOCALLY -> rolle.groupName
            }

        return AzureObjectId(stringId)
    }
}
