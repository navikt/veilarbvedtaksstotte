package no.nav.veilarbvedtaksstotte.repository;

import no.nav.veilarbvedtaksstotte.client.dokument.DokumentSendtDTO;
import no.nav.veilarbvedtaksstotte.domain.vedtak.*;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

import static no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus.GODKJENT_AV_BESLUTTER;
import static no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus.KLAR_TIL_BESLUTTER;
import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.Assert.*;

public class VedtaksstotteRepositoryTest {

    private static JdbcTemplate db;
    private static TransactionTemplate transactor;
    private static KilderRepository kilderRepository;
    private static VedtaksstotteRepository vedtaksstotteRepository;

    @BeforeClass
    public static void setup() {
        db = SingletonPostgresContainer.init().createJdbcTemplate();
        transactor = new TransactionTemplate(new DataSourceTransactionManager(db.getDataSource()));
        kilderRepository = new KilderRepository(db);
        vedtaksstotteRepository = new VedtaksstotteRepository(db, transactor);
    }

    @Before
    public void cleanup() {
        DbTestUtils.cleanupDb(db);
    }

    @Test
    public void hentUtkastEldreEnn_skal_hente_riktig_utkast() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        assertTrue(vedtaksstotteRepository.hentUtkastEldreEnn(LocalDateTime.now().plusSeconds(1)).isEmpty());
        assertEquals(1, vedtaksstotteRepository.hentUtkastEldreEnn(LocalDateTime.now().minusSeconds(5)).size());
    }

    @Test
    public void setGodkjentAvBeslutter_skal_sette_godkjent() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        UtkastetVedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        vedtaksstotteRepository.setBeslutterProsessStatus(utkast.getId(), GODKJENT_AV_BESLUTTER);

        UtkastetVedtak oppdatertUtkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        assertSame(oppdatertUtkast.getBeslutterProsessStatus(), GODKJENT_AV_BESLUTTER);
    }

    @Test
    public void skal_starteBeslutterProsess() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        UtkastetVedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        vedtaksstotteRepository.setBeslutterProsessStatus(utkast.getId(), KLAR_TIL_BESLUTTER);

        UtkastetVedtak oppdatertUtkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        assertSame(oppdatertUtkast.getBeslutterProsessStatus(), KLAR_TIL_BESLUTTER);
    }

    @Test
    public void skal_sette_beslutter() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        UtkastetVedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        vedtaksstotteRepository.setBeslutter(utkast.getId(), TEST_BESLUTTER_IDENT);

        UtkastetVedtak oppdatertUtkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        assertEquals(TEST_BESLUTTER_IDENT, oppdatertUtkast.getBeslutterIdent());
    }

    @Test
    public void setBeslutterProsessStatus__skal_sette_status() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        UtkastetVedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        vedtaksstotteRepository.setBeslutterProsessStatus(utkast.getId(), BeslutterProsessStatus.KLAR_TIL_BESLUTTER);

        UtkastetVedtak oppdatertUtkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        assertEquals(BeslutterProsessStatus.KLAR_TIL_BESLUTTER, oppdatertUtkast.getBeslutterProsessStatus());
    }

    @Test
    public void testSlettUtkast() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        boolean isVedtakSlettet = vedtaksstotteRepository.slettUtkast(utkast.getId());
        assertSame(true, isVedtakSlettet);

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

        DokumentSendtDTO dokumentSendtDTO = new DokumentSendtDTO(TEST_JOURNALPOST_ID, TEST_DOKUMENT_ID);

        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        UtkastetVedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        utkast.setBegrunnelse(TEST_BEGRUNNELSE);
        utkast.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);
        utkast.setHovedmal(Hovedmal.SKAFFE_ARBEID);
        vedtaksstotteRepository.oppdaterUtkast(utkast.getId(), utkast);
        kilderRepository.lagKilder(TEST_KILDER, utkast.getId());

        vedtaksstotteRepository.ferdigstillVedtak(utkast.getId(), dokumentSendtDTO);

        vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(TEST_AKTOR_ID);

        FattetVedtak fattetVedtak = vedtaksstotteRepository.hentFattetEllerFeil(utkast.getId());

        assertNotNull(fattetVedtak);
        assertFalse(fattetVedtak.isGjeldende());

    }

    @Test
    public void test_Ny_Vedtak_Kolon_Mapping() {
        DokumentSendtDTO dokumentSendtDTO = new DokumentSendtDTO(TEST_JOURNALPOST_ID, TEST_DOKUMENT_ID);

        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        UtkastetVedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);
        assertNotNull("UTKAST_SIST_OPPDATERT skal ikke vare null", utkast.getSistOppdatert());
        vedtaksstotteRepository.ferdigstillVedtak(utkast.getId(), dokumentSendtDTO);
        vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(TEST_AKTOR_ID);

        FattetVedtak fattetVedtak = vedtaksstotteRepository.hentFattetEllerFeil(utkast.getId());

        assertNotNull(fattetVedtak);
        assertNotNull("Vedtak Fattet tidspunkt kan ikke vare null", fattetVedtak.getVedtakFattet());
    }
}
