package no.nav.veilarbvedtaksstotte.service

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.OppfolgingPeriodeDTO
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.repository.TestvedtakRepository
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class TestvedtakServiceTest {
    private val testvedtakRepository = mock(TestvedtakRepository::class.java)
    private val kafkaProducerService = mock(KafkaProducerService::class.java)
    private val veilarboppfolgingClient = mock(VeilarboppfolgingClient::class.java)
    private val testvedtakService = TestvedtakService(
        testvedtakRepository,
        kafkaProducerService,
        veilarboppfolgingClient
    )

    @Test
    fun `skal ikke lagre eller publisere identisk testvedtak`() {
        val aktorId = AktorId.of("1234567890123")
        val vedtak = testvedtak(aktorId, begrunnelse = "Begrunnelse")
        `when`(testvedtakRepository.hentGjeldendeTestvedtak(aktorId)).thenReturn(vedtak)

        testvedtakService.lagreTestvedtak(vedtak, Fnr.of("12345678901"))

        verify(testvedtakRepository, never()).settTidligereTestvedtakIkkeGjeldende(aktorId)
        verify(testvedtakRepository, never()).lagreTestvedtak(vedtak)
        verifyNoInteractions(kafkaProducerService, veilarboppfolgingClient)
    }

    @Test
    fun `skal ikke lagre eller publisere nar manglende begrunnelse matcher standardbegrunnelsen`() {
        val aktorId = AktorId.of("1234567890123")
        val vedtak = testvedtak(aktorId, begrunnelse = null)
        `when`(testvedtakRepository.hentGjeldendeTestvedtak(aktorId))
            .thenReturn(testvedtak(aktorId, begrunnelse = TestvedtakRepository.DEFAULT_BEGRUNNELSE))

        testvedtakService.lagreTestvedtak(vedtak, Fnr.of("12345678901"))

        verify(testvedtakRepository, never()).settTidligereTestvedtakIkkeGjeldende(aktorId)
        verify(testvedtakRepository, never()).lagreTestvedtak(vedtak)
        verifyNoInteractions(kafkaProducerService, veilarboppfolgingClient)
    }

    private fun testvedtak(aktorId: AktorId, begrunnelse: String?): Vedtak {
        val tidspunkt = LocalDateTime.now()
        return Vedtak()
            .settAktorId(aktorId.get())
            .settHovedmal(Hovedmal.SKAFFE_ARBEID)
            .settInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS)
            .settOppfolgingsenhetId("1234")
            .settBegrunnelse(begrunnelse)
            .settVeilederIdent("Z123456")
            .settUtkastOpprettet(tidspunkt)
            .settUtkastSistOppdatert(tidspunkt)
            .settVedtakFattet(tidspunkt)
            .settReferanse(UUID.randomUUID())
    }
}
