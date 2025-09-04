package no.nav.veilarbvedtaksstotte.service

import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.veilarbvedtaksstotte.repository.SakStatistikkRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SakStatistikkResendingService(
    private val sakStatistikkRepository: SakStatistikkRepository,
    private val bigQueryService: BigQueryService,
    private val leaderElectionClient: LeaderElectionClient
) {
    /**
     * Eksempel på resending ligger nederst i filen
     *
     * Steg 0: Endre eller lag test i SakStatistikkResendingServiceTest.kt
     * Steg 4: Sett et tidspunkt for når dette skal kjøre og fjern utkommentering av @Scheduled
     */

    //@Scheduled(cron = "0 12 16 28 5 ?")
    fun resendStatistikk() {
        if (leaderElectionClient.isLeader) {
            val log: Logger = LoggerFactory.getLogger(SakStatistikkResendingService::class.java)
            log.info("Starter resending av sakstatistikk")

            // Steg 1: Endre parametere for å hente de radene som skal endres
            val parameters = mapOf<String, Any>()

            // Steg 2: Lag en SQL-spørring for å hente de radene som skal endres og resendes
            val sql = ""

            val sakStatistikkRader = sakStatistikkRepository.hentSakStatistikkListe(sql, parameters)

            // Steg 3: Endrer de nye radene til korrekt verdi, pass på å slette/ikke sette sekvensnummer, det settes automatisk ved innsetting i databasen
            val endredeRader = sakStatistikkRader.map {
                it.copy(
                    sekvensnummer = null
                )
            }

            val lagredeDatabaseRader = sakStatistikkRepository.insertSakStatistikkRadBatch(endredeRader)

            bigQueryService.logEvent(lagredeDatabaseRader)
            log.info("Resending av sakstatistikk fullført, ${endredeRader.size} rader oppdatert og sendt til BigQuery")
        }
    }

    /***
     * Eksempel på Resending:
     *
     * @Scheduled(cron = "0 12 16 28 5 ?")
     * fun resendStatistikk() {
     *     if (leaderElectionClient.isLeader) {
     *         val log: Logger = LoggerFactory.getLogger(SakStatistikkResendingService::class.java)
     *         log.info("Starter resending av sakstatistikk")
     *
     *         val parameters = mapOf<String, Any>("BEHANDLING_TYPE" to BehandlingType.REVURDERING.name)
     *
     *         val sql = "SELECT * FROM $SAK_STATISTIKK_TABLE WHERE mottatt_tid > registrert_tid AND behandling_type=:BEHANDLING_TYPE"
     *
     *         val sakStatistikkRader = sakStatistikkRepository.hentSakStatistikkListe(sql, parameters)
     *
     *         // Lager de nye radene, pass på å slette/ikke sette sekvensnummer, det settes automatisk ved innsetting i databasen
     *         val endredeRader = sakStatistikkRader.map { it ->
     *             it.copy(
     *                 sekvensnummer = null,
     *                 mottattTid = it.registrertTid
     *             )
     *         }
     *
     *         val lagredeDatabaseRader = sakStatistikkRepository.insertSakStatistikkRadBatch(endredeRader)
     *
     *         bigQueryService.logEvent(lagredeDatabaseRader)
     *         log.info("Resending av sakstatistikk fullført, ${endredeRader.size} rader oppdatert og sendt til BigQuery")
     *     }
     * }
     */
}