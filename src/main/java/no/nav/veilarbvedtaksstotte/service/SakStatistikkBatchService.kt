package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class SakStatistikkBatchService @Autowired constructor(
    private val vedtaksstotteRepository: VedtaksstotteRepository,
    private val sakStatistikkService: SakStatistikkService,
    private val authService: AuthService
) {
    private val log: Logger = LoggerFactory.getLogger(SakStatistikkBatchService::class.java)
    // TODO: Endre cron til å kjøre på riktig tidspunkt
    @Scheduled(cron = "0 15 13 5 3 *") // Kjører 5. mars kl 11:30
    fun lastInnFattedeVedtak(){
        log.info("Klar for å laste inn fattede Vedtak")
        val fattedeVedtak: List<Vedtak> = vedtaksstotteRepository.hentFattedeVedtak(10000, 0)
        log.info(String.format("Antall fattedeVedtak: {}"), fattedeVedtak.size)
        fattedeVedtak.forEach { vedtak ->
            try {
                val brukerFnr: Fnr = authService.getFnrOrThrow(vedtak.aktorId)
                sakStatistikkService.fattetVedtak(vedtak, brukerFnr)
            } catch(e: Exception) {
                log.error("Feil med ident:{}, Feil:{}", vedtak.aktorId, e)
            }
        }
    }
}
