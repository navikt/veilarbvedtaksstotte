package no.nav.veilarbvedtaksstotte.repository

import no.nav.common.types.identer.AktorId
import no.nav.veilarbvedtaksstotte.domain.statistikk.SakStatistikk
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import java.time.LocalDateTime
import java.util.*

class SakStatistikkRepositoryTest : DatabaseTest() {
    /*
    class VedtakUtkast(
        val id: String,
        val aktorId: String,
        val veilederIdent: String,
        val oppfolgingsenhetId: String,
    )

     */
    companion object {
        lateinit var sakStatistikkRepository: SakStatistikkRepository

        @BeforeAll
        @JvmStatic
        fun setup(): Unit {
            sakStatistikkRepository = SakStatistikkRepository(jdbcTemplate)
        }
    }

    @Test
    fun `lagre statistikkrad`() {

        val statistikkRad = SakStatistikk(
            behandlingId = 3001.toBigInteger(),
            behandlingUuid = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
            relatertBehandlingId = null,
            relatertFagsystem = null,
            sakId = "Arbeidsoppfølging",
            aktorId = "2004140973848",
            mottattTid = LocalDateTime.now().minusDays(2),
            registrertTid = LocalDateTime.now(),
            ferdigbehandletTid = null,
            endretTid = LocalDateTime.now(),
            tekniskTid = LocalDateTime.now().plusHours(1),
            sakYtelse = null,
            behandlingType = "VEDTAK",
            behandlingStatus = "UTKAST",
            behandlingResultat = null,
            behandlingMetode = "MANUELL",
            opprettetAv = "Z123456",
            saksbehandler = "Z123456",
            ansvarligBeslutter = null,
            ansvarligEnhet = "0220",
            avsender = "Oppfølgingsvedtak § 14 a",
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
            behandlingType = "VEDTAK",
            behandlingStatus = "UTKAST",
            behandlingResultat = null,
            behandlingMetode = "MANUELL",
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
        val lagretStatistikkRadUtkast = behandlingId.let { sakStatistikkRepository.hentSakStatistikkListe(aktorId.toString()) }
        assertEquals(behandlingId.toString(), lagretStatistikkRadUtkast.get(0).behandlingId.toString())}}
/*
    @Test
    fun `lagre statistikkrad utkast2`() {

        val vedtakUtkast = VedtakUtkast(
            id = "1",
            aktorId = "aktor123",
            veilederIdent = "2004140973848",
            oppfolgingsenhetId = "0220"
        )
        sakStatistikkRepository.insertSakStatistikkRadUtkast(vedtakUtkast.id, vedtakUtkast.aktorId, vedtakUtkast.veilederIdent, vedtakUtkast.oppfolgingsenhetId)
        val lagretStatistikkRadUtkast = vedtakUtkast.id.let { sakStatistikkRepository.hentSakStatistikkListe(vedtakUtkast.aktorId.toString()) }
        assertEquals(vedtakUtkast.id.toString(), lagretStatistikkRadUtkast.get(0).behandlingId.toString())}
}
*/