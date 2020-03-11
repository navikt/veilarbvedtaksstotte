package no.nav.veilarbvedtaksstotte.utils;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.repository.KilderRepository;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.util.Arrays;
import java.util.List;

import static no.nav.veilarbvedtaksstotte.repository.KafkaRepository.VEDTAK_SENDT_KAFKA_FEIL_TABLE;
import static no.nav.veilarbvedtaksstotte.repository.OyeblikksbildeRepository.OYEBLIKKSBILDE_TABLE;
import static no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository.VEDTAK_TABLE;

@Slf4j
public class DbTestUtils {

    // Rekkefølgen er viktig pga foreign key constraints
    private final static List<String> ALL_TABLES = Arrays.asList(
            KilderRepository.KILDE_TABLE,
            VEDTAK_SENDT_KAFKA_FEIL_TABLE,
            OYEBLIKKSBILDE_TABLE,
            VEDTAK_TABLE
    );

    public static void testMigrate (DataSource dataSource) {
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

}
