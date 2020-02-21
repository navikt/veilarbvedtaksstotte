package no.nav.fo.veilarbvedtaksstotte.utils;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.util.Arrays;
import java.util.List;

import static no.nav.fo.veilarbvedtaksstotte.repository.KafkaRepository.VEDTAK_SENDT_KAFKA_FEIL_TABLE;
import static no.nav.fo.veilarbvedtaksstotte.repository.KilderRepository.KILDE_TABLE;
import static no.nav.fo.veilarbvedtaksstotte.repository.OyeblikksbildeRepository.OYEBLIKKSBILDE_TABLE;
import static no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository.VEDTAK_TABLE;

@Slf4j
public class DbTestUtils {

    // Rekkefølgen er viktig pga foreign key constraints
    private final static List<String> ALL_TABLES = Arrays.asList(
            KILDE_TABLE,
            VEDTAK_SENDT_KAFKA_FEIL_TABLE,
            OYEBLIKKSBILDE_TABLE,
            VEDTAK_TABLE
    );

    public static JdbcTemplate setupEmbeddedDb(EmbeddedPostgres postgres) {
        DataSource source = postgres.getPostgresDatabase();
        DbTestUtils.testMigrate(source);
        return new JdbcTemplate(source);
    }

    public static void cleanupDb(JdbcTemplate db) {
        ALL_TABLES.forEach((table) -> deleteAllFromTable(db, table));
    }

    public static void testMigrate(DataSource dataSource) {
        log.info("Starting test migration...");
        Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }

    private static void deleteAllFromTable(JdbcTemplate db, String tableName) {
        db.execute("DELETE FROM " + tableName);
    }
}
