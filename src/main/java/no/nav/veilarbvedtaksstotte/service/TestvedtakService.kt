package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.toGjeldende14aVedtakKafkaDTO
import no.nav.veilarbvedtaksstotte.domain.vedtak.toSiste14aVedtak
import no.nav.veilarbvedtaksstotte.repository.TestvedtakRepository
import org.springframework.stereotype.Service

@Service
class TestvedtakService(
    private val testvedtakRepository: TestvedtakRepository,
    private val kafkaProducerService: KafkaProducerService
) {

    fun lagreTestvedtak(vedtak: Vedtak) {
        try {
            testvedtakRepository.lagreTestvedtak(vedtak)
            kafkaProducerService.sendSiste14aVedtak(vedtak.toSiste14aVedtak())
            kafkaProducerService.sendGjeldende14aVedtak(AktorId.of(vedtak.aktorId), vedtak.toGjeldende14aVedtakKafkaDTO())
        } catch (e: Exception) {
            throw RuntimeException("Kunne ikke lagre testvedtak: ${e.message}", e)
        }

    }

    fun hentTestvedtak(aktorId: AktorId): Vedtak? {
        return testvedtakRepository.hentTestvedtak(aktorId)
    }

    fun slettTestvedtak(aktorId: AktorId) {
        try {
            val vedtakForSletting = testvedtakRepository.hentTestvedtak(aktorId)
                ?: throw RuntimeException("Ingen testvedtak funnet for aktorId: $aktorId")
            testvedtakRepository.slettTestvedtak(aktorId)
            //man sender ikke med at vedtak ikke lengre er gjeldende i siste14avedtak så ikke nødvendig å poste en topic-melding her
            kafkaProducerService.sendGjeldende14aVedtak(AktorId.of(vedtakForSletting.aktorId), null)
        } catch (e: Exception) {
            throw RuntimeException("Kunne ikke lagre testvedtak: ${e.message}", e)
        }
    }
}