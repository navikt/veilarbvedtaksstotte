package no.nav.veilarbvedtaksstotte.schedule

import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.service.VedtakService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

@Component
class DistribuerDokumentSchedule(
    val leaderElection: LeaderElectionClient,
    val vedtakService: VedtakService,
    val vedtaksstotteRepository: VedtaksstotteRepository,
    val transactor: TransactionTemplate
) {

    val log = LoggerFactory.getLogger(DistribuerDokumentSchedule::class.java)

    // hvert minutt
    @Scheduled(cron = "0 0/1 * * * *")
    fun distribuerJournalforteDokument() {
        if (leaderElection.isLeader) { // TODO toggle?
            val hentVedtakForDistribusjon: MutableList<Long> = vedtaksstotteRepository.hentVedtakForDistribusjon(10)
            if (hentVedtakForDistribusjon.isNotEmpty()) {
                log.info("Distribuerer ${hentVedtakForDistribusjon.size} vedtak med id: ${hentVedtakForDistribusjon.joinToString(",", "[", "]")}")
            } else {
                log.info("Ingen nye vedtak å distribuere")
            }

            hentVedtakForDistribusjon.forEach {
                try {
                    vedtakService.distribuerVedtak(it)
                } catch (e: RuntimeException) {
                    log.error("Distribusjon av vedtak med id ${it} feilet", e)
                }
            }

        }
    }
}
