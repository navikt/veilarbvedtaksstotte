package no.nav.fo.veilarbvedtaksstotte.utils;

import org.springframework.jdbc.core.JdbcTemplate;

public class DbUtils {

    public static long nesteFraSekvens(String sekvensNavn, JdbcTemplate db) {
        return db.queryForObject("select " + sekvensNavn + ".nextval from dual", Long.class);
    }

}
