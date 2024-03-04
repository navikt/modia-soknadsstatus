package no.nav.modia.soknadsstatus.utils

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.net.InetAddress

interface LeaderElectionService {
    fun isLeader(): Boolean

    fun hostName(): String
}

class LeaderElectionServiceImpl(
    private val electorPath: String?,
) : LeaderElectionService {
    private val httpClient =
        HttpClient(OkHttp) {
        }

    private val logger = LoggerFactory.getLogger(LeaderElectionServiceImpl::class.java)

    override fun hostName(): String = InetAddress.getLocalHost().hostName

    override fun isLeader(): Boolean {
        try {
            if (!electorPath.isNullOrBlank()) {
                val leader =
                    runBlocking {
                        httpClient
                            .get(
                                "http://$electorPath/",
                            ).bodyAsText()
                            .let { Json.parseToJsonElement(it) }
                            .jsonObject["name"]
                            ?.jsonPrimitive
                            ?.content
                    }

                logger.info("Leader election gave leader $leader. Hostname is ${hostName()}. Election result = ${leader == hostName()}")

                return leader == hostName()
            }
        } catch (e: IllegalArgumentException) {
            logger.error("Feil under henting av leader election", e)
        }

        logger.info("Leader election path er ikke satt. Ingen leder blir valgt")
        return false
    }
}
