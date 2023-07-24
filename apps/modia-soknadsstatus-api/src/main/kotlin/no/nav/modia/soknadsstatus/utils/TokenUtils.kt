package no.nav.modia.soknadsstatus.utils

import no.nav.common.token_client.client.MachineToMachineTokenClient
import no.nav.common.token_client.client.OnBehalfOfTokenClient
import no.nav.personoversikt.common.logging.Logging

class DownstreamApi(
    val cluster: String,
    val namespace: String,
    val application: String,
) {
    companion object
}

private fun DownstreamApi.tokenscope(): String = "api://$cluster.$namespace.$application/.default"

fun DownstreamApi.Companion.parse(value: String): DownstreamApi {
    val parts = value.split(":")
    check(parts.size == 3) { "DownstreamApi string must contain 3 parts" }

    val cluster = parts[0]
    val namespace = parts[1]
    val application = parts[2]

    return DownstreamApi(cluster = cluster, namespace = namespace, application = application)
}

fun MachineToMachineTokenClient.createMachineToMachineToken(api: DownstreamApi): String {
    return this.createMachineToMachineToken(api.tokenscope())
}

interface BoundedOnBehalfOfTokenClient {
    fun exchangeOnBehalfOfToken(accesstoken: String): String
}

interface BoundedMachineToMachineTokenClient {
    fun createMachineToMachineToken(): String
}

fun OnBehalfOfTokenClient.bindTo(api: DownstreamApi) = object : BoundedOnBehalfOfTokenClient {
    override fun exchangeOnBehalfOfToken(accesstoken: String) = exchangeOnBehalfOfToken(api.tokenscope(), accesstoken)
}

fun OnBehalfOfTokenClient.bindTo(api: String) = object : BoundedOnBehalfOfTokenClient {
    override fun exchangeOnBehalfOfToken(accesstoken: String) = exchangeOnBehalfOfToken(api, accesstoken)
}

fun MachineToMachineTokenClient.bindTo(api: DownstreamApi) = object : BoundedMachineToMachineTokenClient {
    override fun createMachineToMachineToken() = createMachineToMachineToken(api.tokenscope())
}

fun MachineToMachineTokenClient.bindTo(api: String) = object : BoundedMachineToMachineTokenClient {
    override fun createMachineToMachineToken(): String = api
}
