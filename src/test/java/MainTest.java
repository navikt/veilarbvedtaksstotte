import no.nav.apiapp.ApiApp;
import no.nav.fo.veilarbvedtaksstotte.TestContext;
import no.nav.fo.veilarbvedtaksstotte.config.ApplicationTestConfig;
import no.nav.fo.veilarbvedtaksstotte.db.DatabaseTestContext;
import no.nav.testconfig.ApiAppTest;

import static no.nav.fo.veilarbvedtaksstotte.config.ApplicationConfig.APPLICATION_NAME;

public class MainTest {

    private static final String TEST_PORT = "8812";
    private static final String[] ARGUMENTS = {TEST_PORT};

    public static void main(String[] args) {
        ApiAppTest.setupTestContext(ApiAppTest.Config.builder().applicationName(APPLICATION_NAME).build());
        TestContext.setup();
        DatabaseTestContext.setup("Q1");
        ApiApp.startApiApp(ApplicationTestConfig.class, ARGUMENTS);
    }

}
