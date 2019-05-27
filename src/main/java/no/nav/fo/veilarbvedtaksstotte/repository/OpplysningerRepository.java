package no.nav.fo.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.fo.veilarbvedtaksstotte.domain.Opplysning;
import no.nav.fo.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class OpplysningerRepository {

    private final static String OPPLYSNING_TABLE            = "OPPLYSNING";
    private final static String VEDTAK_ID                   = "VEDTAK_ID";
    private final static String TEKST                       = "TEKST";

    private final JdbcTemplate db;

    @Inject
    public OpplysningerRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void lagOpplysninger(List<String> opplysninger, long vedtakId) {
        opplysninger.forEach((opplysning) -> insertOpplysning(opplysning, vedtakId));
    }

    public List<Opplysning> hentOpplysningerForVedtak(long vedtakId) {
        return SqlUtils.select(db, OPPLYSNING_TABLE, OpplysningerRepository::mapOpplysninger)
                .where(WhereClause.equals(VEDTAK_ID, vedtakId))
                .column("*")
                .executeToList();
    }

    public List<Opplysning> hentOpplysningerForAlleVedtak(List<Vedtak> vedtakListe) {
        List<Long> vedtakIder = vedtakListe.stream().map(Vedtak::getId).collect(Collectors.toList());
        return SqlUtils.select(db, OPPLYSNING_TABLE, OpplysningerRepository::mapOpplysninger)
                .where(WhereClause.in(VEDTAK_ID, vedtakIder))
                .column("*")
                .executeToList();
    }

    public void slettOpplysninger(long vedtakId) {
        SqlUtils.delete(db, OPPLYSNING_TABLE)
                .where(WhereClause.equals(VEDTAK_ID, vedtakId))
                .execute();
    }

    private void insertOpplysning(String tekst, long vedtakId) {
        SqlUtils.insert(db, OPPLYSNING_TABLE)
                .value(VEDTAK_ID, vedtakId)
                .value(TEKST, tekst)
                .execute();
    }

    @SneakyThrows
    private static Opplysning mapOpplysninger(ResultSet rs) {
        return new Opplysning()
                .setVedtakId(rs.getLong(VEDTAK_ID))
                .setTekst(rs.getString(TEKST));
    }

}

