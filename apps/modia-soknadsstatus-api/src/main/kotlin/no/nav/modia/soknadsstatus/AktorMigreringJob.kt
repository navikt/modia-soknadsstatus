package no.nav.modia.soknadsstatus

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class AktorMigreringJob(
    private val services: Services,
) {
    suspend fun start() {
        val logger = LoggerFactory.getLogger(AktorMigreringJob::class.java)

        // Initial delay is 1 second
        var delayTime: Long = 1000

        logger.info("Starter migrering av aktor til FNR")

        while (true) {
            if (services.leaderElectionService.isLeader()) {
                logger.info("Henter nye aktor IDer for migrering til Fnr")
                val aktorIder = services.behandlingEierService.getAktorIdsToConvert(1000)
                if (aktorIder.isEmpty()) continue

                val aktorFnrMapping = services.pdlQ1.hentFnrMedSystemTokenBolk(aktorIder)
                if (aktorFnrMapping.isEmpty()) {
                    logger.warn("Fikk ingen mappinger tilbake fra PDL")
                    continue
                }

                services.behandlingEierService.convertAktorToIdent(aktorFnrMapping)
                services.hendelseEierService.convertAktorToIdent(aktorFnrMapping)

                logger.info("Migrerte aktor ID til FNR for ${aktorFnrMapping.size} elemeter")
            } else {
                logger.info("Instans er ikke leder. Venter 30 sekunder før ny leader sjekk")
                delayTime = 30000
            }

            runBlocking {
                delay(delayTime)
            }
        }
    }
}
