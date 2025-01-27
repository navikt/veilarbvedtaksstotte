package no.nav.veilarbvedtaksstotte.repository

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingMetode
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingResultat
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingStatus
import no.nav.veilarbvedtaksstotte.domain.statistikk.BehandlingType
import no.nav.veilarbvedtaksstotte.domain.statistikk.Fagsystem
import no.nav.veilarbvedtaksstotte.domain.statistikk.SAK_YTELSE
import no.nav.veilarbvedtaksstotte.domain.statistikk.SakStatistikk
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class SakStatistikkRepositoryTest : DatabaseTest() {

    companion object {
        lateinit var sakStatistikkRepository: SakStatistikkRepository

        @BeforeAll
        @JvmStatic
        fun setup() {
            sakStatistikkRepository = SakStatistikkRepository(jdbcTemplate)
        }
    }

    @Test
    fun `lagre statistikkrad vedtak`() {

        val statistikkRad = SakStatistikk(
            aktorId = "2004140973848",
            oppfolgingPeriodeUUID = UUID.fromString("1a930d0d-6931-403e-852c-b85e39673aaf"),
            behandlingId = 3001.toBigInteger(),
            relatertBehandlingId = null,
            relatertFagsystem = null,
            sakId = "Arbeidsoppfølging",
            mottattTid = Instant.now().minus(2, ChronoUnit.DAYS),
            registrertTid = Instant.now().minus(1, ChronoUnit.DAYS),
            ferdigbehandletTid = Instant.now(),
            endretTid = Instant.now(),
            tekniskTid = Instant.now(),
            sakYtelse = SAK_YTELSE,
            behandlingType = BehandlingType.FORSTEGANGSBEHANDLING,
            behandlingStatus = BehandlingStatus.FATTET,
            behandlingResultat = BehandlingResultat.GODE_MULIGHETER,
            behandlingMetode = BehandlingMetode.MANUELL,
            innsatsgruppe = BehandlingResultat.GODE_MULIGHETER,
            hovedmal = Hovedmal.SKAFFE_ARBEID,
            opprettetAv = "Z123456",
            saksbehandler = "Z123456",
            ansvarligBeslutter = "Z123456",
            ansvarligEnhet = EnhetId.of("0220"),
            avsender = Fagsystem.OPPFOLGINGSVEDTAK_14A,
            versjon = "Dockerimage_tag_1"
        )
        sakStatistikkRepository.insertSakStatistikkRad(statistikkRad)
        val lagretStatistikkRadAlt = sakStatistikkRepository.hentSakStatistikkListeAlt(3001.toBigInteger())
        val lagretStatistikkRad = sakStatistikkRepository.hentSakStatistikkListe("2004140973848")
        assertEquals(lagretStatistikkRadAlt.get(0).behandlingId, lagretStatistikkRad.get(1).behandlingId)
    }

    @Test
    fun `lagre statistikkrad utkast`() {
        val statistikkRad2 = SakStatistikk(
            behandlingId = 3002.toBigInteger(),
            relatertBehandlingId = null,
            relatertFagsystem = null,
            sakId = null,
            aktorId = "2004140973848",
            mottattTid = Instant.now().minus(2, ChronoUnit.DAYS),
            registrertTid = Instant.now(),
            ferdigbehandletTid = null,
            endretTid = Instant.now(),
            tekniskTid = Instant.now(),
            sakYtelse = null,
            behandlingType = BehandlingType.FORSTEGANGSBEHANDLING,
            behandlingStatus = BehandlingStatus.UNDER_BEHANDLING,
            behandlingResultat = null,
            behandlingMetode = BehandlingMetode.MANUELL,
            opprettetAv = "Z123456",
            saksbehandler = "Z123456",
            ansvarligBeslutter = null,
            ansvarligEnhet = EnhetId.of("0220"),
            avsender = Fagsystem.OPPFOLGINGSVEDTAK_14A,
            versjon = "Dockerimage_tag_1",
            oppfolgingPeriodeUUID = UUID.fromString("1a930d0d-6931-403e-852c-b85e39673aaf"),
        )
        val aktorId = AktorId.of("2004140973848")
        val behandlingId = 3002.toBigInteger()
        sakStatistikkRepository.insertSakStatistikkRad(statistikkRad2)
        val lagretStatistikkRadUtkast =
            behandlingId.let { sakStatistikkRepository.hentSakStatistikkListe(aktorId.toString()) }
        assertEquals(behandlingId.toString(), lagretStatistikkRadUtkast.get(0).behandlingId.toString())
    }
}
