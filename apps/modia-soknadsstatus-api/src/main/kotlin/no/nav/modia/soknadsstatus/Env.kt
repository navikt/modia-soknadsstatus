package no.nav.modia.soknadsstatus

import no.nav.modia.soknadsstatus.axsys.AxsysEnv
import no.nav.modia.soknadsstatus.ldap.LDAPEnv
import no.nav.modia.soknadsstatus.nom.NomEnv
import no.nav.modia.soknadsstatus.norg.NorgEnv
import no.nav.modia.soknadsstatus.pdl.PdlEnv
import no.nav.modia.soknadsstatus.skjermedepersoner.SkjermedePersonerEnv
import no.nav.modia.soknadsstatus.utils.DownstreamApi
import no.nav.modia.soknadsstatus.utils.parse
import no.nav.personoversikt.common.utils.EnvUtils

interface Env {
    companion object {
        operator fun invoke() = EnvImpl()
    }
    val appMode: AppMode
    val appName: String
    val appVersion: String
    val clusterName: String
    val brokerUrl: String
    val sourceTopic: String
    val azureAdConfiguration: AzureAdConfiguration
    val datasourceConfiguration: DatasourceConfiguration
    val axsysEnv: AxsysEnv
    val ldapEnv: LDAPEnv
    val nomEnv: NomEnv
    val norgEnv: NorgEnv
    val skjermedePersonerEnv: SkjermedePersonerEnv
    val pdlEnv: PdlEnv
}

data class EnvImpl(
    override val appMode: AppMode = AppMode(EnvUtils.getRequiredConfig("APP_MODE", mapOf("APP_MODE" to "NAIS"))),
    override val appName: String = EnvUtils.getRequiredConfig("APP_NAME"),
    override val appVersion: String = EnvUtils.getRequiredConfig("APP_VERSION"),
    override val brokerUrl: String = EnvUtils.getRequiredConfig("KAFKA_BROKER_URL"),
    override val sourceTopic: String = EnvUtils.getRequiredConfig("KAFKA_SOURCE_TOPIC"),
    override val clusterName: String = EnvUtils.getRequiredConfig("NAIS_CLUSTER_NAME"),
    override val datasourceConfiguration: DatasourceConfiguration = DatasourceConfiguration(),
    override val azureAdConfiguration: AzureAdConfiguration = AzureAdConfiguration.load(),
    override val pdlEnv: PdlEnv = PdlEnv(url = EnvUtils.getRequiredConfig("PDL_API_URL"), scope = EnvUtils.getRequiredConfig("PDL_SCOPE")),
    override val axsysEnv: AxsysEnv = AxsysEnv(scope = EnvUtils.getRequiredConfig("AXSYS_SCOPE"), url = EnvUtils.getRequiredConfig("AXSYS_URL")),
    override val ldapEnv: LDAPEnv = LDAPEnv(url = EnvUtils.getRequiredConfig("LDAP_URL"), username = EnvUtils.getRequiredConfig("LDAP_USERNAME"), password = EnvUtils.getRequiredConfig("LDAP_PASSWORD"), baseDN = EnvUtils.getRequiredConfig("LDAP_BASEDN")),
    override val nomEnv: NomEnv = NomEnv(scope = EnvUtils.getRequiredConfig("NOM_SCOPE"), url = EnvUtils.getRequiredConfig("NOM_URL")),
    override val norgEnv: NorgEnv = NorgEnv(url = EnvUtils.getRequiredConfig("NORG2_BASEURL")),
    override val skjermedePersonerEnv: SkjermedePersonerEnv = SkjermedePersonerEnv(url = EnvUtils.getRequiredConfig("SKJERMEDE_PERSONER_PIP_URL"), scope = EnvUtils.getRequiredConfig("SKJERMEDE_PERSONER_SCOPE"))
) : Env
