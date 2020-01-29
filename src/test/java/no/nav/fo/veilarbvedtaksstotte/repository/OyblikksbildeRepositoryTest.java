package no.nav.fo.veilarbvedtaksstotte.repository;

import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import no.nav.fo.veilarbvedtaksstotte.domain.Oyblikksbilde;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.OyblikksbildeType;
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

public class OyblikksbildeRepositoryTest {

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private final static String REGISTRERINGSINFO_JSON = "{ \"data\": 42 }";

    private static JdbcTemplate db;
    private static OyblikksbildeRepository oyblikksbildeRepository;
    private static VedtaksstotteRepository vedtaksstotteRepository;

    @BeforeClass
    public static void setup() {
        db = DbTestUtils.setupDb(pg.getEmbeddedPostgres());
        KilderRepository kilderRepository = new KilderRepository(db);
        oyblikksbildeRepository = new OyblikksbildeRepository(db);
        vedtaksstotteRepository = new VedtaksstotteRepository(db, kilderRepository);
    }

    @Before
    public void cleanup() {
        DbTestUtils.cleanupDb(db);
    }

    @Test
    public void testLagOyblikksbildeFeilerHvisIkkeVedtakFinnes() {
        Oyblikksbilde oyblikksbilde = new Oyblikksbilde(
                VEDTAK_ID_THAT_DOES_NOT_EXIST,
                OyblikksbildeType.REGISTRERINGSINFO,
                REGISTRERINGSINFO_JSON
        );

        List<Oyblikksbilde> oyblikksbilder = Collections.singletonList(oyblikksbilde);

        assertThrows(DataIntegrityViolationException.class, () -> oyblikksbildeRepository.lagOyblikksbilde(oyblikksbilder));
    }

    @Test
    public void testLagOgHentOyblikksbilde() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_VEILEDER_ENHET_ID, TEST_VEILEDER_ENHET_NAVN);
        long vedtakId = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID).getId();

        Oyblikksbilde oyblikksbilde = new Oyblikksbilde(
                vedtakId,
                OyblikksbildeType.REGISTRERINGSINFO,
                REGISTRERINGSINFO_JSON
        );

        oyblikksbildeRepository.lagOyblikksbilde(Collections.singletonList(oyblikksbilde));

        List<Oyblikksbilde> hentetOyblikksbilder = oyblikksbildeRepository.hentOyblikksbildeForVedtak(vedtakId);

        assertTrue(hentetOyblikksbilder.size() > 0);
        assertEquals(oyblikksbilde.getJson(), hentetOyblikksbilder.get(0).getJson());
    }

}
