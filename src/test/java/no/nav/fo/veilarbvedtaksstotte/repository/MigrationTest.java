package no.nav.fo.veilarbvedtaksstotte.repository;

import no.nav.fo.veilarbvedtaksstotte.utils.DbTestUtils;
import org.junit.Test;
import javax.sql.DataSource;

public class MigrationTest {

    @Test
    public void testFlywayMigration() {
        DataSource dataSource = DbTestUtils.createTestDataSource();
        DbTestUtils.testMigrate(dataSource);
    }

}
