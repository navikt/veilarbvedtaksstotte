package no.nav.veilarbvedtaksstotte.utils

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.aktoroppslag.BrukerIdenter
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.IntegrationTestBase
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository.Companion.ARENA_VEDTAK_TABLE
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository.Companion.FNR
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository.Companion.FRA_DATO
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository.Companion.HENDELSE_ID
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository.Companion.HOVEDMAL
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository.Companion.INNSATSGRUPPE
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository.Companion.OPERATION_TIMESTAMP
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository.Companion.REG_USER
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository.Companion.VEDTAK_ID
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import org.apache.commons.lang3.RandomStringUtils
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

abstract class AbstractVedtakIntegrationTest : IntegrationTestBase() {

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
        beslutterIdent: String? = null,
        gammelVedtakId: Long = 1234
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

        val sql =
            """
                INSERT INTO $ARENA_VEDTAK_TABLE ($FNR, $INNSATSGRUPPE, $HOVEDMAL, $FRA_DATO, $REG_USER, $OPERATION_TIMESTAMP, $HENDELSE_ID, $VEDTAK_ID)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT ($FNR) DO UPDATE
                SET $INNSATSGRUPPE        = EXCLUDED.$INNSATSGRUPPE,
                    $HOVEDMAL             = EXCLUDED.$HOVEDMAL,
                    $FRA_DATO             = EXCLUDED.$FRA_DATO,
                    $REG_USER             = EXCLUDED.$REG_USER,
                    $OPERATION_TIMESTAMP  = EXCLUDED.$OPERATION_TIMESTAMP,
                    $HENDELSE_ID          = EXCLUDED.$HENDELSE_ID,
                    $VEDTAK_ID            = EXCLUDED.$VEDTAK_ID
            """

        DatabaseTest.Companion.jdbcTemplate.update(
            sql,
            arenaVedtak.fnr.get(),
            arenaVedtak.innsatsgruppe.name,
            arenaVedtak.hovedmal?.name,
            arenaVedtak.fraDato,
            arenaVedtak.regUser,
            arenaVedtak.operationTimestamp,
            arenaVedtak.hendelseId,
            arenaVedtak.vedtakId
        )

        return arenaVedtak
    }

    fun gittBrukerIdenter(
        antallHistoriskeFnr: Int = 1, antallHistoriskeAktorId: Int = 1
    ): BrukerIdenter {
        val brukerIdenter = BrukerIdenter(
            Fnr(RandomStringUtils.randomNumeric(11)),
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
