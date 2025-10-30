package no.nav.modia.soknadsstatus.azure

import no.nav.common.client.msgraph.AdGroupFilter
import no.nav.common.client.msgraph.MsGraphClient
import no.nav.common.types.identer.EnhetId
import no.nav.modia.soknadsstatus.ansatt.AnsattRolle
import no.nav.modia.soknadsstatus.ansatt.RolleListe
import no.nav.modia.soknadsstatus.utils.BoundedOnBehalfOfTokenClient
import no.nav.personoversikt.common.logging.TjenestekallLogg

interface AzureADService {
    fun hentRollerForVeileder(
        ident: String,
        userToken: String,
    ): List<String>

    fun hentEnheterForVeileder(
        ident: String,
        userToken: String,
    ): List<EnhetId>

    fun hentIntersectRollerForVeileder(
        ident: String,
        userToken: String,
        groups: RolleListe,
    ): RolleListe

    fun hentTemaerForVeileder(
        ident: String,
        userToken: String,
    ): List<String>
}

class AzureADServiceImpl(
    private val msGraphClient: MsGraphClient,
    private val tokenClient: BoundedOnBehalfOfTokenClient,
) : AzureADService {
    private val temaRolePrefix = "0000-GA-TEMA_"
    private val enhetRolePrefix = "0000-GA-ENHET_"

    override fun hentRollerForVeileder(
        ident: String,
        userToken: String,
    ): List<String> {
        val oboToken = tokenClient.exchangeOnBehalfOfToken(userToken)
        return try {
            val response = msGraphClient.hentAdGroupsForUser(oboToken, ident)
            if (response.isEmpty()) {
                TjenestekallLogg.warn("Kall til azureAD feilet", fields = mapOf("Veileder ident" to ident))
                return listOf()
            }
            response.map {
                requireNotNull(it.displayName)
            }
        } catch (e: Exception) {
            TjenestekallLogg.warn("Kall til azureAD feile", throwable = e, fields = mapOf("Veileder ident" to ident))
            return listOf()
        }
    }

    override fun hentEnheterForVeileder(
        ident: String,
        userToken: String,
    ): List<EnhetId> {
        val oboToken = tokenClient.exchangeOnBehalfOfToken(userToken)
        return try {
            val response = msGraphClient.hentAdGroupsForUser(oboToken, ident, AdGroupFilter.ENHET)
            if (response.isEmpty()) {
                TjenestekallLogg.warn("Bruker har ingen AzureAD group for enhet", fields = mapOf("Veileder ident" to ident))
                return listOf()
            }
            response.map {
                requireNotNull(EnhetId(it.displayName.removePrefix(enhetRolePrefix)))
            }
        } catch (e: Exception) {
            TjenestekallLogg.warn("Kall til azureAD feile", throwable = e, fields = mapOf("Veileder ident" to ident))
            return listOf()
        }
    }

    override fun hentIntersectRollerForVeileder(
        ident: String,
        userToken: String,
        groups: RolleListe,
    ): RolleListe {
        val oboToken = tokenClient.exchangeOnBehalfOfToken(userToken)
        return try {
            val response = msGraphClient.hentAdGroupsForUser(oboToken, ident)
            if (response.isEmpty()) {
                TjenestekallLogg.warn("Kall til azureAD feilet", fields = mapOf("Veileder ident" to ident))
                return RolleListe()
            }

            val veiledersRoller =
                response.map {
                    AnsattRolle(
                        gruppeId = requireNotNull(it.id),
                        gruppeNavn = requireNotNull(it.displayName),
                    )
                }

            val commonElements = groups intersect veiledersRoller.toSet()

            RolleListe(commonElements)
        } catch (e: Exception) {
            TjenestekallLogg.warn("Kall til azureAD feile", throwable = e, fields = mapOf("Veileder ident" to ident))
            return RolleListe()
        }
    }

    override fun hentTemaerForVeileder(
        ident: String,
        userToken: String,
    ): List<String> {
        val oboToken = tokenClient.exchangeOnBehalfOfToken(userToken)

        return try {
            val response = msGraphClient.hentAdGroupsForUser(oboToken, ident, AdGroupFilter.TEMA)
            if (response.isEmpty()) {
                TjenestekallLogg.warn("Bruker har ingen AzureAD group for tema", fields = mapOf("Veileder ident" to ident))
                return listOf()
            }
            response.map {
                requireNotNull(it.displayName.removePrefix(temaRolePrefix))
            }
        } catch (e: Exception) {
            TjenestekallLogg.warn("Kall til azureAD feile", throwable = e, fields = mapOf("Veileder ident" to ident))
            return listOf()
        }
    }
}

data class MsGraphEnv(
    val url: String,
    val scope: String,
)
