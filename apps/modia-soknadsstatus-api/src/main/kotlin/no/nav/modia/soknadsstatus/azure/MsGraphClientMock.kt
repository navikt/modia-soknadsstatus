package no.nav.modia.soknadsstatus.azure

import no.nav.common.client.msgraph.AdGroupData
import no.nav.common.client.msgraph.AdGroupFilter
import no.nav.common.client.msgraph.MsGraphClient
import no.nav.common.client.msgraph.UserData
import no.nav.common.health.HealthCheckResult
import no.nav.common.types.identer.AzureObjectId
import no.nav.common.types.identer.EnhetId
import no.nav.modia.soknadsstatus.MockData

class MsGraphClientMock : MsGraphClient {
    override fun checkHealth(): HealthCheckResult = HealthCheckResult.healthy()

    override fun hentUserData(p0: String?): UserData = UserData()

    override fun hentUserDataForGroup(
        p0: String?,
        p1: String?,
    ): MutableList<UserData> = mutableListOf()

    override fun hentUserDataForGroup(
        p0: String?,
        p1: EnhetId?,
    ): MutableList<UserData> = mutableListOf()

    override fun hentAdGroupsForUser(
        p0: String?,
        p1: String?,
    ): MutableList<AdGroupData> =
        mutableListOf(
            AdGroupData(AzureObjectId("11"), "0000-GA-ENHET_${MockData.Veileder.enhet}"),
            AdGroupData(AzureObjectId("22"), "0000-GA-TEMA_${MockData.Veileder.fagområder}"),
        )

    override fun hentAdGroupsForUser(
        p0: String?,
        p1: String?,
        p2: AdGroupFilter?,
    ): MutableList<AdGroupData> {
        if (p2 == AdGroupFilter.TEMA) return mutableListOf(AdGroupData(AzureObjectId("22"), "0000-GA-TEMA_${MockData.Veileder.fagområder}"))
        if (p2 == AdGroupFilter.ENHET) return mutableListOf(AdGroupData(AzureObjectId("11"), "0000-GA-ENHET_${MockData.Veileder.enhet}"))
        return mutableListOf()
    }

    override fun hentAdGroupsForUser(
        p0: String?,
        p1: AdGroupFilter?,
    ): MutableList<AdGroupData> = mutableListOf()

    override fun hentOnPremisesSamAccountName(p0: String?): String = "SamAccountName"

    override fun hentAzureGroupId(
        p0: String?,
        p1: EnhetId?,
    ): String = "0000-GA-ENHET_${MockData.Veileder.enhet}"

    override fun hentAzureIdMedNavIdent(
        p0: String?,
        p1: String?,
    ): String = MockData.Veileder.navIdent
}
