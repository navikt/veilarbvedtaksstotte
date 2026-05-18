package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.toGjeldende14aVedtakKafkaDTO
import no.nav.veilarbvedtaksstotte.domain.vedtak.toSiste14aVedtak
import no.nav.veilarbvedtaksstotte.repository.TestvedtakRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class TestvedtakService(
    private val testvedtakRepository: TestvedtakRepository,
    private val kafkaProducerService: KafkaProducerService,
    private val veilarboppfolgingClient: VeilarboppfolgingClient
) {

    @Transactional
    fun lagreTestvedtak(vedtak: Vedtak, fnr: Fnr, navConsumerId: String) {
        val oppfolgingsperiode = veilarboppfolgingClient.hentGjeldendeOppfolgingsperiode(fnr)
        if (oppfolgingsperiode.isEmpty) {
            veilarboppfolgingClient.startOppfolgingsperiode(fnr)
        }
        testvedtakRepository.settTidligereVedtakIkkeGjeldende(AktorId.of(vedtak.aktorId))
        testvedtakRepository.lagreTestvedtak(vedtak, navConsumerId)
        kafkaProducerService.sendSiste14aVedtak(vedtak.toSiste14aVedtak())
        kafkaProducerService.sendGjeldende14aVedtak(AktorId.of(vedtak.aktorId), vedtak.toGjeldende14aVedtakKafkaDTO())
    }

    fun hentGjeldendeTestvedtak(aktorId: AktorId): Vedtak? {
        return testvedtakRepository.hentGjeldendeTestvedtak(aktorId)
    }

    @Transactional
    fun slettGjeldendeTestvedtak(aktorId: AktorId, navConsumerId: String) {
        testvedtakRepository.hentGjeldendeTestvedtak(aktorId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Ingen gjeldende testvedtak funnet for aktøren ved sletting")
        testvedtakRepository.slettGjeldendeTestvedtak(aktorId, navConsumerId)
        kafkaProducerService.sendGjeldende14aVedtak(aktorId, null)
    }
}
