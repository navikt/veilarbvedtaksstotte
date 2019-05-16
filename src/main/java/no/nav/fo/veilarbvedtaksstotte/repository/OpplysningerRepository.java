package no.nav.fo.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.fo.veilarbvedtaksstotte.domain.AnnenOpplysning;
import no.nav.fo.veilarbvedtaksstotte.domain.Opplysning;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.OpplysningsType;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.List;

import static no.nav.fo.veilarbvedtaksstotte.utils.EnumUtils.valueOf;

@Repository
public class OpplysningerRepository {

    private final static String VEDTAK_ID                   = "VEDTAK_ID";

    private final static String KILDE_TYPE_TABLE            = "KILDE_TYPE";
    private final static String VERDI                       = "VERDI";

    private final static String OPPLYSNING_TABLE            = "OPPLYSNING";
    private final static String KILDE                       = "KILDE";
    private final static String JSON                        = "JSON";

    private final static String ANDRE_OPPLYSNINGER_TABLE    = "ANDRE_OPPLYSNINGER";
    private final static String TEKST                       = "TEKST";

    private final JdbcTemplate db;

    @Inject
    public OpplysningerRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void lagOpplysning(List<Opplysning> opplysninger) {
        opplysninger.forEach(this::lagOpplysning);
    }

    private void lagOpplysning(Opplysning opplysning) {
        SqlUtils.insert(db, OPPLYSNING_TABLE)
                .value(VEDTAK_ID, opplysning.getVedtakId())
                .value(KILDE, opplysning.getOpplysningsType())
                .value(JSON, opplysning.getJson())
                .execute();
    }

    public void lagAnnenOpplysning(List<AnnenOpplysning> opplysninger) {
        opplysninger.forEach(this::lagAnnenOpplysning);
    }

    private void lagAnnenOpplysning(AnnenOpplysning annenOpplysning) {
        SqlUtils.insert(db, ANDRE_OPPLYSNINGER_TABLE)
                .value(VEDTAK_ID, annenOpplysning.getVedtakId())
                .value(TEKST, annenOpplysning.getTekst())
                .execute();
    }

    public List<Opplysning> hentOpplysningerForVedtak(long vedtakId) {
        WhereClause where = WhereClause
                .equals(VEDTAK_ID, vedtakId);

        return SqlUtils.select(db, OPPLYSNING_TABLE, OpplysningerRepository::mapOpplysning)
                .where(where)
                .column("*")
                .executeToList();
    }

    @SneakyThrows
    private static Opplysning mapOpplysning(ResultSet rs) {
        return new Opplysning()
                .setVedtakId(rs.getLong(VEDTAK_ID))
                .setOpplysningsType(valueOf(OpplysningsType.class, rs.getString(KILDE)))
                .setJson(rs.getString(JSON));
    }

    public List<AnnenOpplysning> hentAndreOpplysningerForVedtak(long vedtakId) {
        WhereClause where = WhereClause
                .equals(VEDTAK_ID, vedtakId);

        return SqlUtils.select(db, ANDRE_OPPLYSNINGER_TABLE, OpplysningerRepository::mapAndreOpplysninger)
                .where(where)
                .column("*")
                .executeToList();
    }

    @SneakyThrows
    private static AnnenOpplysning mapAndreOpplysninger(ResultSet rs) {
        return new AnnenOpplysning()
                .setVedtakId(rs.getLong(VEDTAK_ID))
                .setTekst(rs.getString(TEKST));
    }

    public void slettOpplysninger(long vedtakId) {
        SqlUtils.delete(db, OPPLYSNING_TABLE)
                .where(WhereClause.equals(VEDTAK_ID, vedtakId))
                .execute();
    }

    public void slettAndreOpplysninger(long vedtakId) {
        SqlUtils.delete(db, ANDRE_OPPLYSNINGER_TABLE)
                .where(WhereClause.equals(VEDTAK_ID, vedtakId))
                .execute();
    }
}

