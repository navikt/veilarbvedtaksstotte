package no.nav.fo.veilarbvedtaksstotte.db;

import static no.nav.fo.veilarbvedtaksstotte.config.DatabaseConfig.VEILARBVEDTAKSSTOTTE_DB_URL_PROPERTY;

public class DatabaseTestContext {

    private final static String DB_USER = "postgres";
    private final static String DB_PASSWORD = "password";

    public static void setup() {
        System.setProperty(VEILARBVEDTAKSSTOTTE_DB_URL_PROPERTY, createTestDbUrl());
    }

    private static String createTestDbUrl() {
        String baseUrl = "jdbc:h2:mem:veilarbvedtaksstotte";
        String[] urlOptions = new String[]{
                "DB_CLOSE_DELAY=-1",
                "MODE=PostgreSQL",
                "USER=" + DB_USER,
                "PASSWORD=" + DB_PASSWORD
        };

        return baseUrl + ";" + String.join(";", urlOptions);
    }
}
