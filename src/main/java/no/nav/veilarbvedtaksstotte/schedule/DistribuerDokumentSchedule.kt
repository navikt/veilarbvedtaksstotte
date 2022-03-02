package no.nav.veilarbvedtaksstotte.schedule

import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.service.DistribusjonServiceV2
import no.nav.veilarbvedtaksstotte.service.UnleashService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class DistribuerDokumentSchedule(
    val leaderElection: LeaderElectionClient,
    val distribusjonServiceV2: DistribusjonServiceV2,
    val vedtaksstotteRepository: VedtaksstotteRepository,
    val unleashService: UnleashService
) {

    val log = LoggerFactory.getLogger(DistribuerDokumentSchedule::class.java)

    // hvert minutt
    @Scheduled(cron = "0 0/1 * * * *")
    fun distribuerJournalforteDokument() {
        if (leaderElection.isLeader && unleashService.isDokDistScheduleEnabled) {
            val hentVedtakForDistribusjon: MutableList<Long> = vedtaksstotteRepository.hentVedtakForDistribusjon(10)
            if (hentVedtakForDistribusjon.isNotEmpty()) {
                log.info("Distribuerer ${hentVedtakForDistribusjon.size} vedtak med id: ${hentVedtakForDistribusjon.joinToString(",", "{", "}")}")
            } else {
                log.info("Ingen nye vedtak å distribuere")
            }

            hentVedtakForDistribusjon.forEach {
                try {
                    distribusjonServiceV2.distribuerVedtak(it)
                } catch (e: RuntimeException) {
                    log.error("Distribusjon av vedtak med id ${it} feilet", e)
                }
            }

        }
    }
}
