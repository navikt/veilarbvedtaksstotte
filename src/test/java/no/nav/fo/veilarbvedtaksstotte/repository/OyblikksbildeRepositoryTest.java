package no.nav.fo.veilarbvedtaksstotte.repository;

import no.nav.fo.veilarbvedtaksstotte.domain.Oyblikksbilde;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.KildeType;
import no.nav.fo.veilarbvedtaksstotte.utils.DbTestUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;

import static no.nav.fo.veilarbvedtaksstotte.repository.TestData.*;
import static no.nav.fo.veilarbvedtaksstotte.repository.TestData.TEST_VEILEDER_ENHET_NAVN;
import static org.junit.jupiter.api.Assertions.*;

public class OyblikksbildeRepositoryTest {

    private final static String REGISTRERINGSINFO_JSON = "{ \"data\": 42 }";

    private static DataSource testDataSource = DbTestUtils.createTestDataSource();
    private JdbcTemplate db = new JdbcTemplate(testDataSource);
    private OyblikksbildeRepository oyblikksbildeRepository = new OyblikksbildeRepository(db);
    private KilderRepository kilderRepository = new KilderRepository(db);
    private VedtaksstotteRepository vedtaksstotteRepository = new VedtaksstotteRepository(db, kilderRepository);

    @BeforeClass
    public static void setup() {
        DbTestUtils.testMigrate(testDataSource);
    }

    @AfterEach
    public void cleanup() {
        DbTestUtils.cleanupDb(db);
    }

    @Test
    public void testLagOyblikksbildeFeilerHvisIkkeVedtakFinnes() {
        Oyblikksbilde oyblikksbilde = new Oyblikksbilde(
                VEDTAK_ID_THAT_DOES_NOT_EXIST,
                KildeType.REGISTRERINGSINFO,
                REGISTRERINGSINFO_JSON
        );

        List<Oyblikksbilde> oyblikksbilder = Collections.singletonList(oyblikksbilde);

        assertThrows(DataIntegrityViolationException.class, () -> {
            oyblikksbildeRepository.lagOyblikksbilde(oyblikksbilder);
        });
    }

    @Test
    public void testLagOgHentOyblikksbilde() {
        final long vedtakId = 1;

        // Kan ikke opprette kilder hvis det ikke finnes et utkast
        vedtaksstotteRepository.opprettUtakst(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_VEILEDER_ENHET_ID, TEST_VEILEDER_ENHET_NAVN);

        Oyblikksbilde oyblikksbilde = new Oyblikksbilde(
                vedtakId,
                KildeType.REGISTRERINGSINFO,
                REGISTRERINGSINFO_JSON
        );

        List<Oyblikksbilde> oyblikksbilder = Collections.singletonList(oyblikksbilde);

        oyblikksbildeRepository.lagOyblikksbilde(oyblikksbilder);

        List<Oyblikksbilde> hentetOyblikksbilder = oyblikksbildeRepository.hentOyblikksbildeForVedtak(vedtakId);

        assertTrue(hentetOyblikksbilder.size() > 0);
        assertEquals(oyblikksbilde.getJson(), hentetOyblikksbilder.get(0).getJson());
    }

}
