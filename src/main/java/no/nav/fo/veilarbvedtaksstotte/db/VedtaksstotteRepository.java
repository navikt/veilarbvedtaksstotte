package no.nav.fo.veilarbvedtaksstotte.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class VedtaksstotteRepository {

    @Inject
    private final JdbcTemplate db;

    @Inject
    public VedtaksstotteRepository(JdbcTemplate db) {
        this.db = db;
    }

}
