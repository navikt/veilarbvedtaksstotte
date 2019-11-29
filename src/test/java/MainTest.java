import no.nav.apiapp.ApiApp;
import no.nav.fo.veilarbvedtaksstotte.TestContext;
import no.nav.fo.veilarbvedtaksstotte.config.ApplicationTestConfig;
import no.nav.fo.veilarbvedtaksstotte.utils.DbRole;
import no.nav.fo.veilarbvedtaksstotte.utils.DbTestUtils;
import no.nav.fo.veilarbvedtaksstotte.utils.DbUtils;
import no.nav.testconfig.ApiAppTest;

import static no.nav.fo.veilarbvedtaksstotte.config.ApplicationConfig.APPLICATION_NAME;

public class MainTest {

    private static final String TEST_PORT = "8812";
    private static final String[] ARGUMENTS = {TEST_PORT};

    public static void main(String[] args) {
        String dbUrl = "";

        DbUtils.migrateAndClose(DbUtils.createDataSource(dbUrl, DbRole.ADMIN), DbRole.ADMIN);

        ApiAppTest.setupTestContext(ApiAppTest.Config.builder().applicationName(APPLICATION_NAME).build());
        TestContext.setup();
//        DbUtils.migrate(DbTestUtils.createTestDataSource());
        ApiApp.startApiApp(ApplicationTestConfig.class, ARGUMENTS);
    }

}
