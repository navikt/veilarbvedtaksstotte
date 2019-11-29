package no.nav.fo.veilarbvedtaksstotte.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.util.Arrays;
import java.util.List;

import static no.nav.fo.veilarbvedtaksstotte.repository.KafkaRepository.VEDTAK_SENDT_KAFKA_FEIL_TABLE;
import static no.nav.fo.veilarbvedtaksstotte.repository.KilderRepository.KILDE_TABLE;
import static no.nav.fo.veilarbvedtaksstotte.repository.OyblikksbildeRepository.OYBLIKKSBILDE_TABLE;
import static no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository.VEDTAK_TABLE;

public class DbTestUtils {

    public final static String DB_USER = "postgres";
    public final static String DB_PASSWORD = "password";

    private final static List<String> ALL_TABLES = Arrays.asList(
            VEDTAK_TABLE,
            VEDTAK_SENDT_KAFKA_FEIL_TABLE,
            OYBLIKKSBILDE_TABLE,
            KILDE_TABLE
    );

    public static void testMigrate(DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }

    public static void cleanupDb(JdbcTemplate db) {
        ALL_TABLES.forEach((table) -> deleteAllFromTable(db, table));
    }

    public static void deleteAllFromTable(JdbcTemplate db, String tableName) {
        db.execute("DELETE FROM " + tableName);
    }

    public static DataSource createTestDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(createTestDbUrl());
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);

        return new HikariDataSource(config);
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
