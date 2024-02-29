package no.nav.modia.soknadsstatus

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class AktorMigreringJob(
    private val services: Services,
) {
    suspend fun start() {
        val logger = LoggerFactory.getLogger(AktorMigreringJob::class.java)

        if (services.leaderElectionService.isLeader()) {
            logger.info("Starter migrering av aktor til FNR som leader (host: ${services.leaderElectionService.hostName()})")

            while (true) {
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

                runBlocking {
                    delay(1000)
                }
            }
        }
    }
}
