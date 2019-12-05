package no.nav.fo.veilarbvedtaksstotte.utils;

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

    // Rekkefølgen er viktig pga foreign key constraints
    private final static List<String> ALL_TABLES = Arrays.asList(
            KILDE_TABLE,
            VEDTAK_SENDT_KAFKA_FEIL_TABLE,
            OYBLIKKSBILDE_TABLE,
            VEDTAK_TABLE
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

    public static JdbcTemplate startAndMigratePostgresContainer(PostgresContainer container) {
        container.getContainer().start();

//        try {
//            Thread.sleep(5000);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        DataSource dataSource = container.getDataSource();
        DbTestUtils.testMigrate(dataSource);
        return new JdbcTemplate(dataSource);
    }

    private static void deleteAllFromTable(JdbcTemplate db, String tableName) {
        db.execute("DELETE FROM " + tableName);
    }
}
