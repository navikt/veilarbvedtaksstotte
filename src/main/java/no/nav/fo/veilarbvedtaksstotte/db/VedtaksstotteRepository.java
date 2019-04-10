package no.nav.fo.veilarbvedtaksstotte.db;

import no.nav.fo.veilarbvedtaksstotte.domain.VedtakUtkast;
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

    public VedtakUtkast hentVedtakUtakst(String aktorId) {
        return null;
    }


}
