package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.repository.TestvedtakRepository
import org.springframework.stereotype.Service

@Service
class TestvedtakService(
    private val testvedtakRepository: TestvedtakRepository
) {

    fun lagreTestvedtak(vedtak: Vedtak) {
        testvedtakRepository.lagreTestvedtak(vedtak)
    }

    fun hentTestvedtak(aktorId: AktorId): Vedtak? {
        return testvedtakRepository.hentTestvedtak(aktorId)
    }

    fun slettTestvedtak(aktorId: AktorId) {
        testvedtakRepository.slettTestvedtak(aktorId)
    }
}