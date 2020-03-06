package no.nav.fo.veilarbvedtaksstotte.utils;

import no.nav.fo.veilarbvedtaksstotte.utils.DbTestUtils;
public class SingletonePostgresContainer {

    private static PostgresContainer postgresContainer;

    public static PostgresContainer init() {
        if (postgresContainer == null) {
            postgresContainer = new PostgresContainer();
            postgresContainer.getContainer().start();
            DbTestUtils.testMigrate(postgresContainer.getDataSource());
            setupShutdownHook();
        }

        return postgresContainer;
    }

    private static void setupShutdownHook() {
           Runtime.getRuntime().addShutdownHook(new Thread(() -> {
               if (postgresContainer != null && postgresContainer.getContainer().isRunning()) {
                   postgresContainer.getContainer().stop();
               }
        }));
    }
}
