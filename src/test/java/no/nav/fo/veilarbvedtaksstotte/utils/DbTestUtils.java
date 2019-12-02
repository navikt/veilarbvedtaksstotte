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

    private final static String DB_USER = "postgres";
    private final static String DB_PASSWORD = "password";

    private final static boolean USE_LOCAL_POSTGRES = false;

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

    private static void deleteAllFromTable(JdbcTemplate db, String tableName) {
        db.execute("DELETE FROM " + tableName);
    }

    public static DataSource createTestDataSource() {
        return USE_LOCAL_POSTGRES
                ? createLocalPostgresTestDataSource()
                : createInMemoryTestDataSource();
    }

    private static DataSource createInMemoryTestDataSource() {
        return new HikariDataSource(DbUtils.createDataSourceConfig(createInMemoryTestDbUrl()));
    }

    private static DataSource createLocalPostgresTestDataSource() {
        HikariConfig config = DbUtils.createDataSourceConfig(createLocalPostgresTestDbUrl());
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);
        return new HikariDataSource(config);
    }

    private static String createLocalPostgresTestDbUrl() {
        return "jdbc:postgresql://localhost:5432/veilarbvedtaksstotte";
    }

    private static String createInMemoryTestDbUrl() {
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
