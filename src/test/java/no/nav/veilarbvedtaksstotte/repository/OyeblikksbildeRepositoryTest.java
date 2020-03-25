package no.nav.veilarbvedtaksstotte.repository;

import no.nav.sbl.jdbc.Transactor;
import no.nav.veilarbvedtaksstotte.domain.Oyeblikksbilde;
import no.nav.veilarbvedtaksstotte.domain.enums.OyeblikksbildeType;
import no.nav.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import java.util.Collections;
import java.util.List;

import static no.nav.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OyeblikksbildeRepositoryTest {

    private final static String REGISTRERINGSINFO_JSON = "{ \"data\": 42 }";

    private static JdbcTemplate db;
    private static Transactor transactor;
    private static OyeblikksbildeRepository oyeblikksbildeRepository;
    private static VedtaksstotteRepository vedtaksstotteRepository;

    @BeforeClass
    public static void setup() {
        db = SingletonPostgresContainer.init().getDb();
        transactor = new Transactor(new DataSourceTransactionManager(db.getDataSource()));
        oyeblikksbildeRepository = new OyeblikksbildeRepository(db);
        vedtaksstotteRepository = new VedtaksstotteRepository(db, new KilderRepository(db), new DialogRepository(db), transactor);
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
                REGISTRERINGSINFO_JSON
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
                REGISTRERINGSINFO_JSON
        );

        oyeblikksbildeRepository.lagOyeblikksbilde(Collections.singletonList(oyeblikksbilde));

        List<Oyeblikksbilde> hentetOyeblikksbilder = oyeblikksbildeRepository.hentOyeblikksbildeForVedtak(vedtakId);

        assertTrue(hentetOyeblikksbilder.size() > 0);
        assertEquals(oyeblikksbilde.getJson(), hentetOyeblikksbilder.get(0).getJson());
    }

}
