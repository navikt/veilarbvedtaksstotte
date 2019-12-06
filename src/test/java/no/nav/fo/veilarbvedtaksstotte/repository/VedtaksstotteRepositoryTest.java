package no.nav.fo.veilarbvedtaksstotte.repository;

import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import no.nav.fo.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.fo.veilarbvedtaksstotte.utils.DbTestUtils;
import org.junit.*;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;

import static no.nav.fo.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.Assert.*;

@Ignore
public class VedtaksstotteRepositoryTest {

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static JdbcTemplate db;
    private static KilderRepository kilderRepository;
    private static VedtaksstotteRepository vedtaksstotteRepository;

    @BeforeClass
    public static void setup() {
        db = DbTestUtils.setupDb(pg.getEmbeddedPostgres());
        kilderRepository = new KilderRepository(db);
        vedtaksstotteRepository = new VedtaksstotteRepository(db, kilderRepository);
    }

    @After
    public void cleanup() {
        DbTestUtils.cleanupDb(db);
    }

    @Test
    public void testSlettUtkast() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_VEILEDER_ENHET_ID, TEST_VEILEDER_ENHET_NAVN);

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        kilderRepository.lagKilder(Arrays.asList("Kilde1", "Kilde2"), utkast.getId());

        vedtaksstotteRepository.slettUtkast(TEST_AKTOR_ID);

        assertNull(vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID));
    }

    @Test
    public void testHentUtkast() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_VEILEDER_ENHET_ID, TEST_VEILEDER_ENHET_NAVN);

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
