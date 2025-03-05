package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class SakStatistikkBatchService @Autowired constructor(
    private val vedtaksstotteRepository: VedtaksstotteRepository,
    private val sakStatistikkService: SakStatistikkService,
    private val authService: AuthService
) {
    // TODO: Endre cron til å kjøre på riktig tidspunkt
    @Scheduled(cron = "0 30 12 5 3 *") // Kjører 5. mars kl 11:30
    fun lastInnFattedeVedtak(){
        val fattedeVedtak: List<Vedtak> = vedtaksstotteRepository.hentFattedeVedtak(10000, 0)
        fattedeVedtak.forEach { vedtak ->
            val brukerFnr: Fnr = authService.getFnrOrThrow(vedtak.aktorId)
            sakStatistikkService.fattetVedtak(vedtak, brukerFnr) }
    }
}
