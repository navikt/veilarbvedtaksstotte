package no.nav.veilarbvedtaksstotte.utils;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

import static no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository.BESLUTTEROVERSIKT_BRUKER_TABLE;
import static no.nav.veilarbvedtaksstotte.repository.KilderRepository.KILDE_TABLE;
import static no.nav.veilarbvedtaksstotte.repository.MeldingRepository.DIALOG_MELDING_TABLE;
import static no.nav.veilarbvedtaksstotte.repository.MeldingRepository.SYSTEM_MELDING_TABLE;
import static no.nav.veilarbvedtaksstotte.repository.OyeblikksbildeRepository.OYEBLIKKSBILDE_TABLE;
import static no.nav.veilarbvedtaksstotte.repository.RetryVedtakdistribusjonRepository.RETRY_VEDTAKDISTRIBUSJON_TABELL;
import static no.nav.veilarbvedtaksstotte.repository.SakStatistikkRepository.SAK_STATISTIKK_TABLE;
import static no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository.VEDTAK_TABLE;

@Slf4j
public class DbTestUtils {

    // Rekkefølgen er viktig pga foreign key constraints
    private final static List<String> ALL_TABLES = Arrays.asList(
            KILDE_TABLE,
            OYEBLIKKSBILDE_TABLE,
            DIALOG_MELDING_TABLE,
            SYSTEM_MELDING_TABLE,
            BESLUTTEROVERSIKT_BRUKER_TABLE,
            VEDTAK_TABLE,
            "ARENA_VEDTAK",
            SAK_STATISTIKK_TABLE,
            RETRY_VEDTAKDISTRIBUSJON_TABELL
    );

    public static void testMigrate (DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }

    public static void testMigrate (DataSource dataSource, String versionPrefix) {
        Flyway.configure()
                .target(versionPrefix)
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
