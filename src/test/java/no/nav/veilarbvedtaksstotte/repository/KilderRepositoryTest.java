package no.nav.veilarbvedtaksstotte.repository;

import no.nav.sbl.jdbc.Transactor;
import no.nav.veilarbvedtaksstotte.domain.Kilde;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import java.util.Arrays;
import java.util.List;

import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KilderRepositoryTest {

    private static JdbcTemplate db;
    private static Transactor transactor;
    private static KilderRepository kilderRepository;
    private static MeldingRepository meldingRepository;
    private static VedtaksstotteRepository vedtaksstotteRepository;

    @BeforeClass
    public static void setup() {
        db = SingletonPostgresContainer.init().getDb();
        transactor = new Transactor(new DataSourceTransactionManager(db.getDataSource()));
        kilderRepository = new KilderRepository(db);
        meldingRepository = new MeldingRepository(db);
        vedtaksstotteRepository = new VedtaksstotteRepository(db, kilderRepository, transactor);
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

        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        List<String> kilder = Arrays.asList("kilde1", "kilde2");
        kilderRepository.lagKilder(kilder, vedtakId);
        List<Kilde> kilderHentet = kilderRepository.hentKilderForVedtak(vedtakId);

        kilder.forEach(kilde -> {
            assertTrue(kilderHentet.stream().anyMatch(k -> kilde.equals(k.getTekst()) && k.getVedtakId() == vedtakId));
        });
    }

}
