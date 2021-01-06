package no.nav.veilarbvedtaksstotte.repository;

import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.Oyeblikksbilde;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeType;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.List;

import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OyeblikksbildeRepositoryTest {

    private final static String JSON_DATA = "{ \"data\": 42 }";

    private static JdbcTemplate db;
    private static TransactionTemplate transactor;
    private static OyeblikksbildeRepository oyeblikksbildeRepository;
    private static VedtaksstotteRepository vedtaksstotteRepository;

    @BeforeClass
    public static void setup() {
        db = SingletonPostgresContainer.init().getDb();
        transactor = new TransactionTemplate(new DataSourceTransactionManager(db.getDataSource()));
        oyeblikksbildeRepository = new OyeblikksbildeRepository(db);
        vedtaksstotteRepository = new VedtaksstotteRepository(db, transactor);
    }

    @Before
    public void cleanup() {
        DbTestUtils.cleanupDb(db);
    }

    @Test
    public void testLagOyeblikksbildeFeilerHvisIkkeVedtakFinnes() {
        Oyeblikksbilde oyeblikksbilde = new Oyeblikksbilde(
                VEDTAK_ID_THAT_DOES_NOT_EXIST,
                OyeblikksbildeType.REGISTRERINGSINFO,
                JSON_DATA
        );

        List<Oyeblikksbilde> oyeblikksbilder = Collections.singletonList(oyeblikksbilde);

        assertThrows(DataIntegrityViolationException.class, () -> oyeblikksbildeRepository.lagOyeblikksbilde(oyeblikksbilder));
    }

    @Test
    public void testLagOgHentOyeblikksbilde() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        Oyeblikksbilde oyeblikksbilde = new Oyeblikksbilde(
                vedtakId,
                OyeblikksbildeType.REGISTRERINGSINFO,
                JSON_DATA
        );

        oyeblikksbildeRepository.lagOyeblikksbilde(Collections.singletonList(oyeblikksbilde));

        List<Oyeblikksbilde> hentetOyeblikksbilder = oyeblikksbildeRepository.hentOyeblikksbildeForVedtak(vedtakId);

        assertTrue(hentetOyeblikksbilder.size() > 0);
        assertEquals(oyeblikksbilde.getJson(), hentetOyeblikksbilder.get(0).getJson());
    }

    @Test
    public void testSlettOyeblikksbilde() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_OPPFOLGINGSENHET_ID);
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        Oyeblikksbilde oyeblikksbilde1 = new Oyeblikksbilde(
                vedtakId,
                OyeblikksbildeType.REGISTRERINGSINFO,
                JSON_DATA
        );

        Oyeblikksbilde oyeblikksbilde2 = new Oyeblikksbilde(
                vedtakId,
                OyeblikksbildeType.CV_OG_JOBBPROFIL,
                JSON_DATA
        );

        List<Oyeblikksbilde> oyeblikksbilder = List.of(oyeblikksbilde1, oyeblikksbilde2);
        oyeblikksbildeRepository.lagOyeblikksbilde(oyeblikksbilder);

        oyeblikksbildeRepository.slettOyeblikksbilder(vedtakId);

        assertTrue(oyeblikksbildeRepository.hentOyeblikksbildeForVedtak(vedtakId).isEmpty());
    }

}
