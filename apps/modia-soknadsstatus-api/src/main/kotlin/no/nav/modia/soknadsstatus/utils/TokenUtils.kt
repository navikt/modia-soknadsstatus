package no.nav.modia.soknadsstatus.utils

import no.nav.common.token_client.client.OnBehalfOfTokenClient

class DownstreamApi(
    val cluster: String,
    val namespace: String,
    val application: String,
) { companion object }

private fun DownstreamApi.tokenscope(): String = "api://$cluster.$namespace.$application/.default"

fun DownstreamApi.Companion.parse(value: String): DownstreamApi {
    val parts = value.split(":")
    check(parts.size == 3) { "DownstreamApi string must contain 3 parts" }

    val cluster = parts[0]
    val namespace = parts[1]
    val application = parts[2]

    return DownstreamApi(cluster = cluster, namespace = namespace, application = application)
}

interface BoundedOnBehalfOfTokenClient {
    fun exchangeOnBehalfOfToken(accesstoken: String): String
}

fun OnBehalfOfTokenClient.bindTo(api: DownstreamApi) = object : BoundedOnBehalfOfTokenClient {
    override fun exchangeOnBehalfOfToken(accesstoken: String) = exchangeOnBehalfOfToken(api.tokenscope(), accesstoken)
}