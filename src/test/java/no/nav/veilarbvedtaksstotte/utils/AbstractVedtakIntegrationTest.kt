package no.nav.veilarbvedtaksstotte.utils

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.aktoroppslag.BrukerIdenter
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.config.ApplicationTestConfig
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import org.apache.commons.lang3.RandomStringUtils
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import kotlin.random.Random

@SpringBootTest(classes = [ApplicationTestConfig::class])
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class AbstractVedtakIntegrationTest {

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var vedtakRepository: VedtaksstotteRepository

    @Autowired
    lateinit var arenaVedtakRepository: ArenaVedtakRepository

    @MockBean
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
        vedtakRepository.settGjeldendeVedtakTilHistorisk(aktorId.get(), ZonedDateTime.now())
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


    private val defaultArenaVedtakFraDato = LocalDate.now()
    private val defaultArenaVedtakRegUser = "REG USER"
    private val defaultArenaVedtakInnsatsgruppe = ArenaVedtak.ArenaInnsatsgruppe.BFORM
    private val defaultArenaVedtakHovedmal = ArenaVedtak.ArenaHovedmal.SKAFFEA
    private fun defaultArenaVedtakOperationTimestamp() = LocalDateTime.now()
    private fun defaultArenaVedtakHendelseId() = Random.nextLong()


    fun arenaVedtak(
        fnr: Fnr,
        fraDato: LocalDate = defaultArenaVedtakFraDato,
        regUser: String = defaultArenaVedtakRegUser,
        innsatsgruppe: ArenaVedtak.ArenaInnsatsgruppe = defaultArenaVedtakInnsatsgruppe,
        hovedmal: ArenaVedtak.ArenaHovedmal = defaultArenaVedtakHovedmal,
        operationTimestamp: LocalDateTime = defaultArenaVedtakOperationTimestamp(),
        hendelseId: Long = defaultArenaVedtakHendelseId()
    ): ArenaVedtak {
        return ArenaVedtak(
            fnr = fnr,
            innsatsgruppe = innsatsgruppe,
            hovedmal = hovedmal,
            fraDato = fraDato,
            regUser = regUser,
            operationTimestamp = operationTimestamp,
            hendelseId = hendelseId,
            vedtakId = 1
        )
    }

    fun lagreArenaVedtak(
        fnr: Fnr,
        fraDato: LocalDate = defaultArenaVedtakFraDato,
        regUser: String = defaultArenaVedtakRegUser,
        innsatsgruppe: ArenaVedtak.ArenaInnsatsgruppe = defaultArenaVedtakInnsatsgruppe,
        hovedmal: ArenaVedtak.ArenaHovedmal = defaultArenaVedtakHovedmal,
        operationTimestamp: LocalDateTime = defaultArenaVedtakOperationTimestamp(),
        hendelseId: Long = defaultArenaVedtakHendelseId()
    ): ArenaVedtak {

        val arenaVedtak = ArenaVedtak(
            fnr = fnr,
            innsatsgruppe = innsatsgruppe,
            hovedmal = hovedmal,
            fraDato = fraDato,
            regUser = regUser,
            operationTimestamp = operationTimestamp,
            hendelseId = hendelseId,
            vedtakId = 1
        )
        arenaVedtakRepository.upsertVedtak(arenaVedtak)

        return arenaVedtak
    }

    fun gittBrukerIdenter(
        antallHistoriskeFnr: Int = 1, antallHistoriskeAktorId: Int = 1
    ): BrukerIdenter {
        val brukerIdenter = BrukerIdenter(Fnr(RandomStringUtils.randomNumeric(11)),
            AktorId(RandomStringUtils.randomNumeric(13)),
            (1..antallHistoriskeFnr).map { Fnr(RandomStringUtils.randomNumeric(11)) },
            (1..antallHistoriskeAktorId).map { AktorId(RandomStringUtils.randomNumeric(11)) })

        `when`(aktorOppslagClient.hentIdenter(ArgumentMatchers.argThat { arg ->
            brukerIdenter.historiskeFnr.plus(brukerIdenter.historiskeAktorId).plus(brukerIdenter.fnr)
                .plus(brukerIdenter.aktorId).contains(arg)
        })).thenReturn(brukerIdenter)

        return brukerIdenter
    }
}
