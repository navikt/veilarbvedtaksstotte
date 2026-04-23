package no.nav.veilarbvedtaksstotte.utils

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.IntegrationTestBase
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime

abstract class AbstractVedtakIntegrationTest : IntegrationTestBase() {

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var vedtakRepository: VedtaksstotteRepository

    @Autowired
    lateinit var arenaVedtakRepository: ArenaVedtakRepository

    @MockitoBean
    lateinit var aktorOppslagClient: AktorOppslagClient

    fun lagreFattetVedtak(
        aktorId: AktorId,
        innsatsgruppe: Innsatsgruppe = Innsatsgruppe.STANDARD_INNSATS,
        hovedmal: Hovedmal = Hovedmal.SKAFFE_ARBEID,
        vedtakFattetDato: LocalDateTime = LocalDateTime.now(),
        gjeldende: Boolean = true,
        enhetId: String = "1234",
        veilederIdent: String = "VIDENT",
        beslutterIdent: String? = null
    ): Vedtak {
        vedtakRepository.opprettUtkast(
            aktorId.get(), TestData.TEST_VEILEDER_IDENT, TestData.TEST_OPPFOLGINGSENHET_ID
        )
        val vedtak = vedtakRepository.hentUtkast(aktorId.get())
        vedtak.innsatsgruppe = innsatsgruppe
        vedtak.hovedmal = hovedmal
        vedtakRepository.oppdaterUtkast(vedtak.id, vedtak)
        vedtakRepository.hentGjeldendeVedtak(aktorId.get())
            ?.also { vedtakRepository.settGjeldendeVedtakTilHistorisk(it.id) }
        vedtakRepository.ferdigstillVedtak(vedtak.id)
        jdbcTemplate.update(
            """
                UPDATE VEDTAK 
                SET VEDTAK_FATTET = ?, GJELDENDE = ?, OPPFOLGINGSENHET_ID = ?, VEILEDER_IDENT = ?, BESLUTTER_IDENT = ? 
                WHERE ID = ?
            """, vedtakFattetDato, gjeldende, enhetId, veilederIdent, beslutterIdent, vedtak.id
        )

        return vedtakRepository.hentVedtak(vedtak.id)
    }
}
