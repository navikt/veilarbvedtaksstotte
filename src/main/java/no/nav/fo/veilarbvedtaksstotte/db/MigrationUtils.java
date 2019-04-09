package no.nav.fo.veilarbvedtaksstotte.db;

import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;

public class MigrationUtils {
    public static void migrate(JdbcTemplate jdbcTemplate) {
        System.out.println("MIGRATING");
        Flyway flyway = new Flyway();
        flyway.setDataSource(jdbcTemplate.getDataSource());
        flyway.migrate();
    }
}
