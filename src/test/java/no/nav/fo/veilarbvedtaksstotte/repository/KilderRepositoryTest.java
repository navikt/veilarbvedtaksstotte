package no.nav.fo.veilarbvedtaksstotte.repository;

import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import no.nav.fo.veilarbvedtaksstotte.domain.Kilde;
import no.nav.fo.veilarbvedtaksstotte.utils.DbTestUtils;
import org.junit.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;

import static no.nav.fo.veilarbvedtaksstotte.utils.TestData.*;
import static no.nav.fo.veilarbvedtaksstotte.utils.TestData.TEST_VEILEDER_ENHET_NAVN;
import static org.junit.jupiter.api.Assertions.*;

@Ignore
public class KilderRepositoryTest {

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

    @Before
    public void cleanup() {
        DbTestUtils.cleanupDb(db);
    }

    @Test
    public void testLagKilderFeilerHvisIkkeVedtakFinnes() {
        List<String> kilder = Arrays.asList("kilde1", "kilde2");

        assertThrows(DataIntegrityViolationException.class, () -> {
            kilderRepository.lagKilder(kilder, VEDTAK_ID_THAT_DOES_NOT_EXIST);
        });
    }

    @Test
    public void testLagOgHentKilder() {

        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_VEILEDER_ENHET_ID, TEST_VEILEDER_ENHET_NAVN);

        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        List<String> kilder = Arrays.asList("kilde1", "kilde2");
        kilderRepository.lagKilder(kilder, vedtakId);
        List<Kilde> kilderHentet = kilderRepository.hentKilderForVedtak(vedtakId);

        kilder.forEach(kilde -> {
            assertTrue(kilderHentet.stream().anyMatch(k -> kilde.equals(k.getTekst()) && k.getVedtakId() == vedtakId));
        });
    }

}
