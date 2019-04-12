package no.nav.fo.veilarbvedtaksstotte.utils;

import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;

public class MigrationUtils {
    public static void migrate(JdbcTemplate jdbcTemplate) {
        Flyway flyway = new Flyway();
        flyway.setDataSource(jdbcTemplate.getDataSource());
        flyway.setBaselineOnMigrate(true);
        flyway.migrate();
    }
}
