package no.nav.fo.veilarbvedtaksstotte.db;

import no.nav.fo.veilarbvedtaksstotte.utils.DbTestUtils;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterEach;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

public class OyblikksbildeRepositoryTest {

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



}
