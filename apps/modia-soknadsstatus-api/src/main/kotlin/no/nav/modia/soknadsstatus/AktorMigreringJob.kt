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
                delayTime = 1000

                try {
                    logger.info("Henter nye aktor IDer for migrering til Fnr")
                    val aktorIder = services.behandlingEierService.getAktorIdsToConvert(1000)
                    if (aktorIder.isEmpty()) continue

                    val aktorFnrMapping = services.pdlMigrering.hentFnrMedSystemTokenBolk(aktorIder)
                    if (aktorFnrMapping.isEmpty()) {
                        logger.warn("Fikk ingen mappinger tilbake fra PDL")
                        continue
                    }

                    logger.info("Konverterer aktor_id til ident for behandling_eiere (${aktorFnrMapping.size}/${aktorIder.size} elementer)")
                    val behandlingEierRes = services.behandlingEierService.convertAktorToIdent(aktorFnrMapping)
                    logger.info(
                        "[behandling_eiere] Slettet ${behandlingEierRes.deleteCount} rader. " +
                            "Oppdaterte ${behandlingEierRes.updateCount} rader",
                    )

                    logger.info("Konverterer aktor_id til ident for hendelse_eiere (${aktorFnrMapping.size} elementer)")
                    val hendelseEierRes = services.hendelseEierService.convertAktorToIdent(aktorFnrMapping)
                    logger.info(
                        "[hendelse_eiere] Slettet ${hendelseEierRes.deleteCount} rader. Oppdaterte ${hendelseEierRes.updateCount} rader",
                    )

                    logger.info("Migrerte aktor ID til FNR for ${aktorFnrMapping.size} elemeter")
                } catch (e: Exception) {
                    logger.error("Feilet under oppdatering av aktor_id til ident", e)
                    delayTime = 60000
                }
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
