package no.nav.veilarbvedtaksstotte.repository;

import no.nav.veilarbvedtaksstotte.domain.Kilde;
import no.nav.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.List;

import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

public class KilderRepositoryTest {

    private static JdbcTemplate db;
    private static TransactionTemplate transactor;
    private static KilderRepository kilderRepository;
    private static MeldingRepository meldingRepository;
    private static VedtaksstotteRepository vedtaksstotteRepository;

    @BeforeClass
    public static void setup() {
        db = SingletonPostgresContainer.init().getDb();
        transactor = new TransactionTemplate(new DataSourceTransactionManager(db.getDataSource()));
        kilderRepository = new KilderRepository(db);
        meldingRepository = new MeldingRepository(db);
        vedtaksstotteRepository = new VedtaksstotteRepository(db, kilderRepository, transactor);
    }

    @Before
    public void cleanup() {
        DbTestUtils.cleanupDb(db);
    }

    @Test
    public void skal_feile_hvis_vedtak_ikke_finnes() {
        List<String> kilder = Arrays.asList("kilde1", "kilde2");

        assertThrows(DataIntegrityViolationException.class, () -> {
            kilderRepository.lagKilder(kilder, VEDTAK_ID_THAT_DOES_NOT_EXIST);
        });
    }

    @Test
    public void skal_lage_og_hente_kilder() {

        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        List<String> kilder = Arrays.asList("kilde1", "kilde2");
        kilderRepository.lagKilder(kilder, vedtakId);
        List<Kilde> kilderHentet = kilderRepository.hentKilderForVedtak(vedtakId);

        kilder.forEach(kilde -> {
            assertTrue(kilderHentet.stream().anyMatch(k -> kilde.equals(k.getTekst()) && k.getVedtakId() == vedtakId));
        });
    }

    @Test
    public void hentKilderForAlleVedtak__skal_hente_alle_kilder() {
        vedtaksstotteRepository.opprettUtkast("aktor1", TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        vedtaksstotteRepository.opprettUtkast("aktor2", TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);

        Vedtak vedtak1 = vedtaksstotteRepository.hentUtkast("aktor1");
        Vedtak vedtak2 = vedtaksstotteRepository.hentUtkast("aktor2");

        List<String> kilder = Arrays.asList("kilde1", "kilde2");

        kilderRepository.lagKilder(kilder, vedtak1.getId());
        kilderRepository.lagKilder(kilder, vedtak2.getId());

        List<Kilde> kilderHentet = kilderRepository.hentKilderForAlleVedtak(List.of(vedtak1, vedtak2));

        assertEquals(4, kilderHentet.size());
    }

}
