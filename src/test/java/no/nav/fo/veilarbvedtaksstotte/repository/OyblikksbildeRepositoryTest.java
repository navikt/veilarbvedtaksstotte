package no.nav.fo.veilarbvedtaksstotte.repository;

import no.nav.fo.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.fo.veilarbvedtaksstotte.utils.SingletonPostgresContainer;
import org.junit.*;
import org.springframework.jdbc.core.JdbcTemplate;

public class OyblikksbildeRepositoryTest {

    private final static String REGISTRERINGSINFO_JSON = "{ \"data\": 42 }";

    private static JdbcTemplate db;
    private static OyblikksbildeRepository oyblikksbildeRepository;
    private static VedtaksstotteRepository vedtaksstotteRepository;

    @BeforeClass
    public static void setup() {
        db = SingletonPostgresContainer.init().getDb();

        KilderRepository kilderRepository = new KilderRepository(db);
        oyblikksbildeRepository = new OyblikksbildeRepository(db);
        vedtaksstotteRepository = new VedtaksstotteRepository(db, kilderRepository);
    }

    @After
    public void cleanup() {
        DbTestUtils.cleanupDb(db);
    }


//    @Test
//    public void testLagOyblikksbildeFeilerHvisIkkeVedtakFinnes() {
//        Oyblikksbilde oyblikksbilde = new Oyblikksbilde(
//                VEDTAK_ID_THAT_DOES_NOT_EXIST,
//                KildeType.REGISTRERINGSINFO,
//                REGISTRERINGSINFO_JSON
//        );
//
//        List<Oyblikksbilde> oyblikksbilder = Collections.singletonList(oyblikksbilde);
//
//        assertThrows(PSQLException.class, () -> oyblikksbildeRepository.lagOyblikksbilde(oyblikksbilder));
//    }

//    @Test
//    public void testLagOgHentOyblikksbilde() {
//        final long vedtakId = 1;
//
//        // Kan ikke opprette kilder hvis det ikke finnes et utkast
//        vedtaksstotteRepository.opprettUtakst(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_VEILEDER_ENHET_ID, TEST_VEILEDER_ENHET_NAVN);
//
//        Oyblikksbilde oyblikksbilde = new Oyblikksbilde(
//                vedtakId,
//                KildeType.REGISTRERINGSINFO,
//                REGISTRERINGSINFO_JSON
//        );
//
//        List<Oyblikksbilde> oyblikksbilder = Collections.singletonList(oyblikksbilde);
//
//        oyblikksbildeRepository.lagOyblikksbilde(oyblikksbilder);
//
//        List<Oyblikksbilde> hentetOyblikksbilder = oyblikksbildeRepository.hentOyblikksbildeForVedtak(vedtakId);
//
//        assertTrue(hentetOyblikksbilder.size() > 0);
//        assertEquals(oyblikksbilde.getJson(), hentetOyblikksbilder.get(0).getJson());
//    }

}
