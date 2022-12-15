package no.nav.veilarbvedtaksstotte.repository;

import no.nav.common.types.identer.AktorId;
import no.nav.veilarbvedtaksstotte.domain.DistribusjonBestillingId;
import no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.utils.DatabaseTest;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus.GODKJENT_AV_BESLUTTER;
import static no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus.KLAR_TIL_BESLUTTER;
import static no.nav.veilarbvedtaksstotte.domain.vedtak.VedtakStatus.SENDT;
import static no.nav.veilarbvedtaksstotte.domain.vedtak.VedtakStatus.UTKAST;
import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.Assert.*;

public class VedtaksstotteRepositoryTest extends DatabaseTest {

    private static KilderRepository kilderRepository;
    private static VedtaksstotteRepository vedtaksstotteRepository;

    @BeforeAll
    public static void setup() {
        kilderRepository = new KilderRepository(jdbcTemplate);
        vedtaksstotteRepository = new VedtaksstotteRepository(jdbcTemplate, transactor);
    }

    @BeforeEach
    public void cleanup() {
        DbTestUtils.cleanupDb(jdbcTemplate);
    }

    @Test
    public void hentUtkastEldreEnn_skal_hente_riktig_utkast() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        assertTrue(vedtaksstotteRepository.hentUtkastEldreEnn(LocalDateTime.now().minusSeconds(5)).isEmpty());
        assertEquals(1, vedtaksstotteRepository.hentUtkastEldreEnn(LocalDateTime.now().plusSeconds(1)).size());
    }

    @Test
    public void setGodkjentAvBeslutter_skal_sette_godkjent() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        vedtaksstotteRepository.setBeslutterProsessStatus(utkast.getId(), GODKJENT_AV_BESLUTTER);

        Vedtak oppdatertUtkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        assertEquals(true, oppdatertUtkast.getBeslutterProsessStatus() == GODKJENT_AV_BESLUTTER);
    }

    @Test
    public void skal_starteBeslutterProsess() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        vedtaksstotteRepository.setBeslutterProsessStatus(utkast.getId(), KLAR_TIL_BESLUTTER);

        Vedtak oppdatertUtkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        assertEquals(true,oppdatertUtkast.getBeslutterProsessStatus() == KLAR_TIL_BESLUTTER);
    }

    @Test
    public void skal_sette_beslutter() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        vedtaksstotteRepository.setBeslutter(utkast.getId(), TEST_BESLUTTER_IDENT);

        Vedtak oppdatertUtkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        assertEquals(TEST_BESLUTTER_IDENT, oppdatertUtkast.getBeslutterIdent());
    }

    @Test
    public void setBeslutterProsessStatus__skal_sette_status() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        vedtaksstotteRepository.setBeslutterProsessStatus(utkast.getId(), BeslutterProsessStatus.KLAR_TIL_BESLUTTER);

        Vedtak oppdatertUtkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        assertEquals(BeslutterProsessStatus.KLAR_TIL_BESLUTTER, oppdatertUtkast.getBeslutterProsessStatus());
    }

    @Test
    public void testSlettUtkast() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        vedtaksstotteRepository.slettUtkast(utkast.getId());

        assertNull(vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID));
    }

    @Test
    public void testOpprettOgHentUtkast() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        assertEquals(TEST_AKTOR_ID, utkast.getAktorId());
        assertEquals(TEST_VEILEDER_IDENT, utkast.getVeilederIdent());
        assertEquals(TEST_OPPFOLGINGSENHET_ID, utkast.getOppfolgingsenhetId());
    }

    @Test
    public void testHentUtkastHvisIkkeFinnes() {
        assertNull(vedtaksstotteRepository.hentUtkast("54385638405"));
    }

    @Test
    public void testSettGjeldendeTilHistorisk() {

        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        utkast.setBegrunnelse(TEST_BEGRUNNELSE);
        utkast.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);
        utkast.setHovedmal(Hovedmal.SKAFFE_ARBEID);
        vedtaksstotteRepository.oppdaterUtkast(utkast.getId(), utkast);
        kilderRepository.lagKilder(TEST_KILDER, utkast.getId());

        vedtaksstotteRepository.ferdigstillVedtak(utkast.getId());

        vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(TEST_AKTOR_ID);

        Vedtak fattetVedtak = vedtaksstotteRepository.hentVedtak(utkast.getId());

        assertNotNull(fattetVedtak);
        assertFalse(fattetVedtak.isGjeldende());

    }

    @Test
    public void lagrer_journalforing_av_vedtak() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        vedtaksstotteRepository.lagreJournalforingVedtak(utkast.getId(), TEST_JOURNALPOST_ID, TEST_DOKUMENT_ID);

        Vedtak oppdatertUtkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        assertEquals(TEST_JOURNALPOST_ID, oppdatertUtkast.getJournalpostId());
        assertEquals(TEST_DOKUMENT_ID, oppdatertUtkast.getDokumentInfoId());
    }

    @Test
    public void lagrer_dokumentbestillings_id() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        vedtaksstotteRepository.lagreDokumentbestillingsId(utkast.getId(), new DistribusjonBestillingId.Uuid(TEST_DOKUMENT_BESTILLING_ID));

        Vedtak oppdatertUtkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        assertEquals(TEST_DOKUMENT_BESTILLING_ID, oppdatertUtkast.getDokumentbestillingId());
    }

    @Test
    public void oppretter_referanse_bare_en_gang() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        UUID referanse1 = vedtaksstotteRepository.opprettOgHentReferanse(utkast.getId());
        UUID referanse2 = vedtaksstotteRepository.opprettOgHentReferanse(utkast.getId());

        assertEquals(referanse1, referanse2);
    }

    @Test
    public void setter_vedtak_fattet_dato_ved_ferdigstilling() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);
        assertNotNull(utkast.getUtkastSistOppdatert());
        vedtaksstotteRepository.ferdigstillVedtak(utkast.getId());

        Vedtak fattetVedtak = vedtaksstotteRepository.hentVedtak(utkast.getId());

        assertNotNull(fattetVedtak);
        assertNotNull(fattetVedtak.getVedtakFattet());
    }

    @Test
    public void henterSisteVedtak() {
        String sql =
                "INSERT INTO VEDTAK(AKTOR_ID, VEILEDER_IDENT, OPPFOLGINGSENHET_ID, STATUS, UTKAST_SIST_OPPDATERT, VEDTAK_FATTET)"
                        + " values(?, ?, ?, ?, ?, ?)";
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(sql, TEST_AKTOR_ID, "veileder1", TEST_OPPFOLGINGSENHET_ID, SENDT.name(), now.minusDays(2), now.minusDays(2));
        jdbcTemplate.update(sql, TEST_AKTOR_ID, "veileder2", TEST_OPPFOLGINGSENHET_ID, SENDT.name(), now.minusDays(1), now.minusDays(1));
        jdbcTemplate.update(sql, TEST_AKTOR_ID, "veileder3", TEST_OPPFOLGINGSENHET_ID, SENDT.name(), now.minusDays(3), now.minusDays(3));
        jdbcTemplate.update(sql, TEST_AKTOR_ID, "veileder4", TEST_OPPFOLGINGSENHET_ID, UTKAST.name(), now, now);

        Vedtak vedtak = vedtaksstotteRepository.hentSisteVedtak(AktorId.of(TEST_AKTOR_ID));

        assertNotNull(vedtak);
        assertEquals("veileder2", vedtak.getVeilederIdent());
    }
}
