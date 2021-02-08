package no.nav.veilarbvedtaksstotte.utils;

public class SingletonPostgresContainer {

    private static PostgresContainer postgresContainer;

    public static PostgresContainer init() {
        if (postgresContainer == null) {
            postgresContainer = new PostgresContainer();
            DbTestUtils.testMigrate(postgresContainer.getDataSource());
            setupShutdownHook();
        }

        return postgresContainer;
    }

    public static PostgresContainer init(String versjonPrefix) {
        if (postgresContainer == null) {
            postgresContainer = new PostgresContainer();
            postgresContainer.getContainer().start();
            DbTestUtils.testMigrate(postgresContainer.getDataSource(), versjonPrefix);
            setupShutdownHook();
        }

        return postgresContainer;
    }

    private static void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (postgresContainer != null) {
                postgresContainer.stopContainer();
            }
        }));
    }
}
