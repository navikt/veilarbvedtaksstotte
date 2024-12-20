package no.nav.veilarbvedtaksstotte.repository

import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.domain.statistikk.SakStatistikk
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import java.time.LocalDateTime
import java.util.*

class SakStatistikkRepositoryTest : DatabaseTest() {

    //TODO: Sjekke testdekning, og se om testene er gode nok
    companion object {
        lateinit var sakStatistikkRepository: SakStatistikkRepository

        @BeforeAll
        @JvmStatic
        fun setup(): Unit {
            sakStatistikkRepository = SakStatistikkRepository(jdbcTemplate)
        }
    }

    @Test
    fun `lagre statistikkrad vedtak`() {

        val statistikkRad = SakStatistikk(
            aktorId = "2004140973848",
            oppfolgingPeriodeUUID = UUID.fromString("1a930d0d-6931-403e-852c-b85e39673aaf"),
            behandlingId = 3001.toBigInteger(),
            behandlingUuid = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
            relatertBehandlingId = null,
            relatertFagsystem = null,
            sakId = "Arbeidsoppfølging",
            mottattTid = LocalDateTime.now().minusDays(2),
            registrertTid = LocalDateTime.now().minusDays(1),
            ferdigbehandletTid = LocalDateTime.now(),
            endretTid = LocalDateTime.now(),
            tekniskTid = LocalDateTime.now().plusHours(1),
            sakYtelse = "BIST14A_IKVAL",
            behandlingType = SakStatistikk.BehandlingType.VEDTAK.name,
            behandlingStatus = SakStatistikk.BehandlingStatus.SENDT.name,
            behandlingResultat = Innsatsgruppe.STANDARD_INNSATS.name,
            behandlingMetode = SakStatistikk.BehandlingMetode.MANUELL.name,
            innsatsgruppe = SakStatistikk.Innsatsgruppe.STANDARD_INNSATS.name,
            hovedmal = SakStatistikk.Hovedmal.SKAFFE_ARBEID.name,
            opprettetAv = "Z123456",
            saksbehandler = "Z123456",
            ansvarligBeslutter = "Z123456",
            ansvarligEnhet = "0220",
            avsender = "Oppfølgingsvedtak § 14 a",
            versjon = "Dockerimage_tag_1"
        )
        sakStatistikkRepository.insertSakStatistikkRad(statistikkRad)
        val lagretStatistikkRadAlt = sakStatistikkRepository.hentSakStatistikkListeAlt(3001.toBigInteger())
        val lagretStatistikkRad = sakStatistikkRepository.hentSakStatistikkListe("2004140973848")
        assertEquals(/* expected = */ lagretStatistikkRadAlt.get(0).behandlingId, /* actual = */ lagretStatistikkRad.get(1).behandlingId)
    }

    @Test
    fun `lagre statistikkrad utkast`() {
        val statistikkRad2 = SakStatistikk(
            behandlingId = 3002.toBigInteger(),
            behandlingUuid = null,
            relatertBehandlingId = null,
            relatertFagsystem = null,
            sakId = null,
            aktorId = "2004140973848",
            mottattTid = LocalDateTime.now().minusDays(2),
            registrertTid = LocalDateTime.now(),
            ferdigbehandletTid = null,
            endretTid = LocalDateTime.now(),
            tekniskTid = LocalDateTime.now().plusHours(1),
            sakYtelse = null,
            behandlingType = SakStatistikk.BehandlingType.VEDTAK.name,
            behandlingStatus = SakStatistikk.BehandlingStatus.UTKAST.name,
            behandlingResultat = null,
            behandlingMetode = SakStatistikk.BehandlingMetode.MANUELL.name,
            innsatsgruppe = null,
            hovedmal = null,
            opprettetAv = "Z123456",
            saksbehandler = "Z123456",
            ansvarligBeslutter = null,
            ansvarligEnhet = "0220",
            avsender = "Oppfølgingsvedtak § 14 a",
            versjon = "Dockerimage_tag_1"
        )
        val aktorId = AktorId.of("2004140973848")
        val behandlingId = 3002.toBigInteger()
        sakStatistikkRepository.insertSakStatistikkRad(statistikkRad2)
        val lagretStatistikkRadUtkast =
            behandlingId.let { sakStatistikkRepository.hentSakStatistikkListe(aktorId.toString()) }
        assertEquals(behandlingId.toString(), lagretStatistikkRadUtkast.get(0).behandlingId.toString())
    }
}
