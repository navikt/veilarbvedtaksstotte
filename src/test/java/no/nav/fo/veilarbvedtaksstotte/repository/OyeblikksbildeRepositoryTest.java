package no.nav.fo.veilarbvedtaksstotte.repository;

import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import no.nav.fo.veilarbvedtaksstotte.domain.Oyeblikksbilde;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.OyeblikksbildeType;
import no.nav.fo.veilarbvedtaksstotte.utils.DbTestUtils;
import org.junit.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.List;

import static no.nav.fo.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OyeblikksbildeRepositoryTest {

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private final static String REGISTRERINGSINFO_JSON = "{ \"data\": 42 }";

    private static JdbcTemplate db;
    private static OyeblikksbildeRepository oyeblikksbildeRepository;
    private static VedtaksstotteRepository vedtaksstotteRepository;

    @BeforeClass
    public static void setup() {
        db = DbTestUtils.setupEmbeddedDb(pg.getEmbeddedPostgres());
        KilderRepository kilderRepository = new KilderRepository(db);
        oyeblikksbildeRepository = new OyeblikksbildeRepository(db);
        vedtaksstotteRepository = new VedtaksstotteRepository(db, kilderRepository);
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
