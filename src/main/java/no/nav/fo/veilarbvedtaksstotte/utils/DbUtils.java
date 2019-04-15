package no.nav.fo.veilarbvedtaksstotte.utils;

import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;

public class DbUtils {

    private DbUtils(){}

    public static void migrate(JdbcTemplate jdbcTemplate) {
        Flyway flyway = new Flyway();
        flyway.setDataSource(jdbcTemplate.getDataSource());
        flyway.setBaselineOnMigrate(true);
        flyway.migrate();
    }

    public static long nesteFraSekvens(JdbcTemplate db, String sekvensNavn) {
        return db.queryForObject(String.format("select %s.nextval from dual", sekvensNavn), Long.class);
    }

}
