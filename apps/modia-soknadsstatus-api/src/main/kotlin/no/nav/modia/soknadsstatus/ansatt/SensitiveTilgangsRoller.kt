package no.nav.modia.soknadsstatus.ansatt

import no.nav.common.types.identer.AzureObjectId
import no.nav.modia.soknadsstatus.kafka.AppCluster

private const val KODE6_PROD = "ad7b87a6-9180-467c-affc-20a566b0fec0"
private const val KODE6_PREPROD = "5ef775f2-61f8-4283-bf3d-8d03f428aa14"
private const val KODE7_PROD = "9ec6487d-f37a-4aad-a027-cd221c1ac32b"
private const val KODE7_PREPROD = "ea930b6b-9397-44d9-b9e6-f4cf527a632a"
private const val SKJERMEDE_PERSONER_PROD = "e750ceb5-b70b-4d94-b4fa-9d22467b786b"
private const val SKJERMEDE_PERSONER_PREPROD = "dbe4ad45-320b-4e9a-aaa1-73cca4ee124d"

private enum class SensitiveRoller(
    val groupName: String,
    val productionRole: AzureObjectId,
    val preprodRole: AzureObjectId,
) {
    KODE6(
        groupName = "0000-GA-STRENGT_FORTROLIG_ADRESSE",
        productionRole = AzureObjectId(KODE6_PROD),
        preprodRole = AzureObjectId(KODE6_PREPROD),
    ),
    KODE7(
        groupName = "0000-GA-FORTROLIG_ADRESSE",
        productionRole = AzureObjectId(KODE7_PROD),
        preprodRole = AzureObjectId(KODE7_PREPROD),
    ),
    SKJERMEDE_PERSONER(
        groupName = "0000-GA-EGNE_ANSATTE",
        productionRole = AzureObjectId(SKJERMEDE_PERSONER_PROD),
        preprodRole = AzureObjectId(SKJERMEDE_PERSONER_PREPROD),
        ),
}

class SensitiveTilgangsRoller(appCluster: AppCluster) {
    val kode6 = getRoleForCluster(appCluster, SensitiveRoller.KODE6)
    val kode7 = getRoleForCluster(appCluster, SensitiveRoller.KODE7)
    val skjermedePersoner = getRoleForCluster(appCluster, SensitiveRoller.SKJERMEDE_PERSONER)

    private fun getRoleForCluster(appCluster: AppCluster, role: SensitiveRoller): AnsattRolle {
        return when (appCluster) {
            AppCluster.PROD -> AnsattRolle(role.groupName, role.productionRole)
            AppCluster.PREPROD -> AnsattRolle(role.groupName, role.preprodRole)
            AppCluster.LOCALLY -> AnsattRolle(role.groupName, AzureObjectId(role.groupName))
        }
    }
}
