package no.nav.veilarbvedtaksstotte.repository

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.veilarbvedtaksstotte.domain.statistikk.*
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class SakStatistikkRepositoryTest : DatabaseTest() {

    companion object {
        lateinit var sakStatistikkRepository: SakStatistikkRepository

        @BeforeAll
        @JvmStatic
        fun setupOnce() {
            sakStatistikkRepository = SakStatistikkRepository(jdbcTemplate)
        }

    }

    @BeforeEach
    fun setup() {
        DbTestUtils.cleanupDb(jdbcTemplate)
    }

    @Test
    fun `lagre statistikkrad vedtak`() {

        val statistikkRad = SakStatistikk(
            aktorId = AktorId.of("2004140973848"),
            oppfolgingPeriodeUUID = UUID.fromString("1a930d0d-6931-403e-852c-b85e39673aaf"),
            behandlingId = 3001.toBigInteger(),
            relatertBehandlingId = 3000.toBigInteger(),
            relatertFagsystem = null,
            sakId = "Arbeidsoppfølging",
            mottattTid = Instant.now().minus(2, ChronoUnit.DAYS),
            registrertTid = Instant.now().minus(1, ChronoUnit.DAYS),
            ferdigbehandletTid = Instant.now(),
            endretTid = Instant.now(),
            sakYtelse = SAK_YTELSE,
            behandlingType = BehandlingType.FORSTEGANGSBEHANDLING,
            behandlingStatus = BehandlingStatus.FATTET,
            behandlingResultat = BehandlingResultat.GODE_MULIGHETER,
            behandlingMetode = BehandlingMetode.MANUELL,
            innsatsgruppe = BehandlingResultat.GODE_MULIGHETER,
            hovedmal = HovedmalNy.BEHOLDE_ARBEID,
            opprettetAv = "Z123456",
            saksbehandler = "Z123456",
            ansvarligBeslutter = "Z123456",
            ansvarligEnhet = EnhetId.of("0220"),
            fagsystemNavn = Fagsystem.OPPFOLGINGSVEDTAK_14A,
            fagsystemVersjon = "Dockerimage_tag_1"
        )
        sakStatistikkRepository.insertSakStatistikkRad(statistikkRad)
        val lagretStatistikkRadAlt = sakStatistikkRepository.hentSakStatistikkListeAlt(3001.toBigInteger())
        val lagretStatistikkRad = sakStatistikkRepository.hentSakStatistikkListe("2004140973848")
        assertEquals(lagretStatistikkRadAlt[0].behandlingId, lagretStatistikkRad[0].behandlingId)
    }

    @Test
    fun `lagre statistikkrad utkast`() {
        val statistikkRad2 = SakStatistikk(
            behandlingId = 3002.toBigInteger(),
            relatertBehandlingId = null,
            relatertFagsystem = null,
            sakId = null,
            aktorId = AktorId.of("2004140973848"),
            mottattTid = Instant.now().minus(2, ChronoUnit.DAYS),
            registrertTid = Instant.now(),
            ferdigbehandletTid = null,
            endretTid = Instant.now(),
            sakYtelse = null,
            behandlingType = BehandlingType.FORSTEGANGSBEHANDLING,
            behandlingStatus = BehandlingStatus.UNDER_BEHANDLING,
            behandlingResultat = null,
            behandlingMetode = BehandlingMetode.MANUELL,
            opprettetAv = "Z123456",
            saksbehandler = "Z123456",
            ansvarligBeslutter = null,
            ansvarligEnhet = EnhetId.of("0220"),
            fagsystemNavn = Fagsystem.OPPFOLGINGSVEDTAK_14A,
            fagsystemVersjon = "Dockerimage_tag_1",
            oppfolgingPeriodeUUID = UUID.fromString("1a930d0d-6931-403e-852c-b85e39673aaf"),
        )
        val aktorId = AktorId.of("2004140973848")
        val behandlingId = 3002.toBigInteger()
        val sekvensnummer = sakStatistikkRepository.insertSakStatistikkRad(statistikkRad2)
        val lagretStatistikkRadUtkast =
            behandlingId.let { sakStatistikkRepository.hentSakStatistikkListe(aktorId.toString()) }
        assertEquals(sekvensnummer, lagretStatistikkRadUtkast[0].sekvensnummer)
        assertEquals(behandlingId.toString(), lagretStatistikkRadUtkast[0].behandlingId.toString())
    }

    @Test
    fun `lagre statistikkrad-batch vedtak`() {
        val aktorId1 = AktorId.of("2004140973848")
        val aktorId2 = AktorId.of("2004140973849")


        val statistikkRad = SakStatistikk(
            aktorId = aktorId1,
            oppfolgingPeriodeUUID = UUID.fromString("1a930d0d-6931-403e-852c-b85e39673aaf"),
            behandlingId = 3001.toBigInteger(),
            relatertBehandlingId = 3000.toBigInteger(),
            relatertFagsystem = null,
            sakId = "Arbeidsoppfølging",
            mottattTid = Instant.now().minus(2, ChronoUnit.DAYS),
            registrertTid = Instant.now().minus(1, ChronoUnit.DAYS),
            ferdigbehandletTid = Instant.now(),
            endretTid = Instant.now(),
            sakYtelse = SAK_YTELSE,
            behandlingType = BehandlingType.FORSTEGANGSBEHANDLING,
            behandlingStatus = BehandlingStatus.FATTET,
            behandlingResultat = BehandlingResultat.GODE_MULIGHETER,
            behandlingMetode = BehandlingMetode.MANUELL,
            innsatsgruppe = BehandlingResultat.GODE_MULIGHETER,
            hovedmal = HovedmalNy.BEHOLDE_ARBEID,
            opprettetAv = "Z123456",
            saksbehandler = "Z123456",
            ansvarligBeslutter = "Z123456",
            ansvarligEnhet = EnhetId.of("0220"),
            fagsystemNavn = Fagsystem.OPPFOLGINGSVEDTAK_14A,
            fagsystemVersjon = "Dockerimage_tag_1"
        )
        val statistikkrader = listOf<SakStatistikk>(statistikkRad, statistikkRad.copy(aktorId = aktorId2, behandlingId = 3002.toBigInteger()))

        sakStatistikkRepository.insertSakStatistikkRadBatch(statistikkrader)
        val lagretStatistikkRad1 = sakStatistikkRepository.hentSakStatistikkListeAlt(3001.toBigInteger())
        val lagretStatistikkRad2 = sakStatistikkRepository.hentSakStatistikkListeAlt(3002.toBigInteger())
        assertEquals(lagretStatistikkRad1[0].aktorId, aktorId1)
        assertEquals(lagretStatistikkRad2[0].aktorId, aktorId2)
    }
}
