package no.nav.veilarbvedtaksstotte.service

import no.nav.common.utils.EnvironmentUtils.isDevelopment
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingResultat
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingStatus
import no.nav.veilarbvedtaksstotte.repository.SakStatistikkRepository
import no.nav.veilarbvedtaksstotte.repository.SakStatistikkRepository.Companion.SAK_STATISTIKK_TABLE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class SakStatistikkResendingService(
    private val sakStatistikkRepository: SakStatistikkRepository,
    private val bigQueryService: BigQueryService
) {
    // 27. mai kl 13:25:00
    @Scheduled(cron = "0 30 13 27 5 ?")
    fun resendStatistikk() {
        if (isDevelopment().get()) {
            return
        }

        val log: Logger = LoggerFactory.getLogger(SakStatistikkResendingService::class.java)
        log.info("Starter resending av sakstatistikk med behandling_status='AVBRUTT'")

        val parameters = mapOf<String, Any>("BEHANDLING_STATUS" to BehandlingStatus.AVBRUTT.name)

        val sql = "SELECT * FROM $SAK_STATISTIKK_TABLE WHERE behandling_status = :BEHANDLING_STATUS"

        val sakStatistikkRader = sakStatistikkRepository.hentSakStatistikkListe(sql, parameters)

        // Lager de nye radene, pass på å slette/ikke sette sekvensnummer, det settes automatisk ved innsetting i databasen
        val endredeRader = sakStatistikkRader.map { it ->
            it.copy(
                sekvensnummer = null,
                behandlingStatus = BehandlingStatus.AVSLUTTET,
                behandlingResultat = BehandlingResultat.AVBRUTT
            )
        }

        val lagredeDatabaseRader = sakStatistikkRepository.insertSakStatistikkRadBatch(endredeRader)

        bigQueryService.logEvent(lagredeDatabaseRader)
        log.info("Resending av sakstatistikk med behandling_status='AVBRUTT' ferdig")
    }

}