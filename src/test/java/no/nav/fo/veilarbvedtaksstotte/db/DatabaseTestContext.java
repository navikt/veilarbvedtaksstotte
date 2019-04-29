package no.nav.fo.veilarbvedtaksstotte.db;

import lombok.val;
import no.nav.fasit.DbCredentials;
import no.nav.fasit.FasitUtils;
import no.nav.fasit.TestEnvironment;

import java.util.Optional;

import static no.nav.fo.veilarbvedtaksstotte.config.ApplicationConfig.APPLICATION_NAME;
import static no.nav.fo.veilarbvedtaksstotte.config.DatabaseConfig.*;

public class DatabaseTestContext {

    public static void setup(String miljo) {
        val dbCredential = Optional.ofNullable(miljo)
                .map(TestEnvironment::valueOf)
                .map(testEnvironment -> FasitUtils.getDbCredentials(testEnvironment, APPLICATION_NAME));

        if (dbCredential.isPresent()) {
            setDataSourceProperties(dbCredential.get());
        } else {
            setInMemoryDataSourceProperties();
        }
    }

    private static void setDataSourceProperties(DbCredentials dbCredentials) {
        System.setProperty(VEILARBVEDTAKSSTOTTE_DB_URL, dbCredentials.url);
        System.setProperty(VEILARBVEDTAKSSTOTTE_DB_USERNAME, dbCredentials.getUsername());
        System.setProperty(VEILARBVEDTAKSSTOTTE_DB_PASSWORD, dbCredentials.getPassword());
    }

    private static void setInMemoryDataSourceProperties() {
        System.setProperty(VEILARBVEDTAKSSTOTTE_DB_URL,
                "jdbc:h2:mem:veilarbvedtaksstotte;DB_CLOSE_DELAY=-1;MODE=Oracle");
        System.setProperty(VEILARBVEDTAKSSTOTTE_DB_USERNAME, "sa");
        System.setProperty(VEILARBVEDTAKSSTOTTE_DB_PASSWORD, "password");
    }

    public static boolean isInMemoryDatabase() {
        return System.getProperty(VEILARBVEDTAKSSTOTTE_DB_URL).startsWith("jdbc:h2:mem:");
    }
}
