package no.nav.veilarbvedtaksstotte.service

import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingType
import no.nav.veilarbvedtaksstotte.repository.SakStatistikkRepository
import no.nav.veilarbvedtaksstotte.repository.SakStatistikkRepository.Companion.SAK_STATISTIKK_TABLE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class SakStatistikkResendingService(
    private val sakStatistikkRepository: SakStatistikkRepository,
    private val bigQueryService: BigQueryService,
    private val leaderElectionClient: LeaderElectionClient
) {
    @Scheduled(cron = "0 12 16 28 5 ?")
    fun resendStatistikk() {
        if (leaderElectionClient.isLeader) {
            val log: Logger = LoggerFactory.getLogger(SakStatistikkResendingService::class.java)
            log.info("Starter resending av sakstatistikk")

            // Steg 1 endre parametere for å hente de radene som skal endres
            val parameters = mapOf<String, Any>("BEHANDLING_TYPE" to BehandlingType.REVURDERING.name)

            val sql = "SELECT * FROM $SAK_STATISTIKK_TABLE WHERE mottatt_tid > registrert_tid AND behandling_type=:BEHANDLING_TYPE"

            val sakStatistikkRader = sakStatistikkRepository.hentSakStatistikkListe(sql, parameters)

            // Lager de nye radene, pass på å slette/ikke sette sekvensnummer, det settes automatisk ved innsetting i databasen
            val endredeRader = sakStatistikkRader.map { it ->
                it.copy(
                    sekvensnummer = null,
                    mottattTid = it.registrertTid
                )
            }

            val lagredeDatabaseRader = sakStatistikkRepository.insertSakStatistikkRadBatch(endredeRader)

            bigQueryService.logEvent(lagredeDatabaseRader)
            log.info("Resending av sakstatistikk fullført, ${endredeRader.size} rader oppdatert og sendt til BigQuery")
        }
    }
}