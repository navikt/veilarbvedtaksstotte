package no.nav.veilarbvedtaksstotte.schedule

import no.nav.common.job.JobRunner
import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.service.DistribusjonService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class DistribuerJournalforteVedtakSchedule(
    val leaderElection: LeaderElectionClient,
    val distribusjonService: DistribusjonService,
    val vedtaksstotteRepository: VedtaksstotteRepository,
) {

    val log = LoggerFactory.getLogger(DistribuerJournalforteVedtakSchedule::class.java)

    companion object {
        @JvmStatic var batchSize: Int = 100
    }

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    fun distribuerJournalforteVedtak() {
        if (leaderElection.isLeader) {
            JobRunner.run("distribuer_journalforte_vedtak") {

                val vedtakForDistribusjon: MutableList<Long> = vedtaksstotteRepository.hentVedtakForDistribusjon(batchSize)

                if (vedtakForDistribusjon.isEmpty()) {
                    log.info("Ingen nye vedtak å distribuere")
                } else {
                    log.info(
                        "Distribuerer ${vedtakForDistribusjon.size} vedtak med følgende IDer: ${
                            vedtakForDistribusjon.joinToString(", ", "{", "}")
                        }"
                    )

                    vedtakForDistribusjon.forEach {
                        try {
                            distribusjonService.distribuerVedtak(it)
                        } catch (e: RuntimeException) {
                            log.error("Distribusjon av vedtak med id $it feilet", e)
                        }
                    }
                }
            }
        }
    }

    @Scheduled(cron = "0 0 12 * * ?") // Hver dag kl. 12
    fun distribuerJournalforteFeilendeVedtak() {
        if (leaderElection.isLeader) {
            JobRunner.run("distribuer_journalforte_feilende_vedtak") {

                val feilendeVedtakTilDistribusjon: MutableList<Long> = vedtaksstotteRepository.hentFeilendeVedtakForDistribusjon(batchSize)

                if (feilendeVedtakTilDistribusjon.isEmpty()) {
                    log.info("Ingen feilende vedtak å distribuere")
                } else {
                    log.info(
                        "Distribuerer ${feilendeVedtakTilDistribusjon.size} vedtak som tidligere har feilet, med følgende IDer: ${
                            feilendeVedtakTilDistribusjon.joinToString(", ", "{", "}")
                        }"
                    )

                    feilendeVedtakTilDistribusjon.forEach {
                        try {
                            distribusjonService.distribuerVedtak(it)
                        } catch (e: RuntimeException) {
                            log.error("Distribusjon av vedtak med id $it feilet", e)
                        }
                    }
                }
            }
        }
    }
}
