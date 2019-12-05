package no.nav.fo.veilarbvedtaksstotte.repository;

import no.nav.fo.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.fo.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.fo.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import org.junit.*;

import org.springframework.jdbc.core.JdbcTemplate;

import static no.nav.fo.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class VedtaksstotteRepositoryTest {

    private static JdbcTemplate db;
    private static VedtaksstotteRepository vedtaksstotteRepository;

    @BeforeClass
    public static void setup() {
        db = SingletonPostgresContainer.init().getDb();

        KilderRepository kilderRepository = new KilderRepository(db);
        vedtaksstotteRepository = new VedtaksstotteRepository(db, kilderRepository);
    }

    @After
    public void cleanup() {
        DbTestUtils.cleanupDb(db);
    }

    @Test
    public void testHentUtkast() {
        vedtaksstotteRepository.opprettUtakst(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_VEILEDER_ENHET_ID, TEST_VEILEDER_ENHET_NAVN);

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        assertEquals(TEST_AKTOR_ID, utkast.getAktorId());
        assertEquals(TEST_VEILEDER_IDENT, utkast.getVeilederIdent());
        assertEquals(TEST_VEILEDER_ENHET_ID, utkast.getVeilederEnhetId());
        assertEquals(TEST_VEILEDER_ENHET_NAVN, utkast.getVeilederEnhetNavn());
    }

    @Test
    public void testHentUtkastHvisIkkeFinnes() {
        assertNull(vedtaksstotteRepository.hentUtkast("54385638405"));
    }

}
