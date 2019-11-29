package no.nav.fo.veilarbvedtaksstotte.repository;

import no.nav.fo.veilarbvedtaksstotte.utils.DbTestUtils;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterEach;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class KilderRepositoryTest {

    private static DataSource testDataSource = DbTestUtils.createTestDataSource();
    private JdbcTemplate db = new JdbcTemplate(testDataSource);

    @BeforeClass
    public static void setup() {
        DbTestUtils.testMigrate(testDataSource);
    }

    @AfterEach
    public void cleanup() {
        DbTestUtils.cleanupDb(db);
    }

//    @Test
//    public void testLagKilderFeilerHvisIkkeVedtakFinnes() {
//        KilderRepository kilderRepository = new KilderRepository(db);
//
//        long vedtakId = 1;
//        List<String> kilder = Arrays.asList("kilde1", "kilde2");
//
//        assertThrows(DataIntegrityViolationException.class, () -> {
//            kilderRepository.lagKilder(kilder, vedtakId);
//        });
//    }

//    @Test
//    public void testLagOgHentKilder() {
//        KilderRepository kilderRepository = new KilderRepository(db);
//
//        // TODO: Insert vedtak
//
//        long vedtakId = 1;
//        List<String> kilder = Arrays.asList("kilde1", "kilde2");
//
//        kilderRepository.lagKilder(kilder, vedtakId);
//    }

}
