package no.nav.fo.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.fo.veilarbvedtaksstotte.domain.AndreOpplysninger;
import no.nav.fo.veilarbvedtaksstotte.domain.Opplysning;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.OpplysningsType;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.List;

import static no.nav.fo.veilarbvedtaksstotte.utils.DbUtils.nesteFraSekvens;
import static no.nav.fo.veilarbvedtaksstotte.utils.EnumUtils.valueOf;

@Repository
public class OpplysningerRepository {

    private final static String VEDTAK_ID                   = "VEDTAK_ID";

    private final static String KILDE_TYPE_TABLE            = "KILDE_TYPE";
    private final static String VERDI                       = "VERDI";

    private final static String OPPLYSNING_TABLE            = "OPPLYSNING";
    private final static String OPPLYSNING_SEQ              = "OPPLYSNING_SEQ";
    private final static String OPPLYSNING_ID               = "OPPLYSNING_ID";
    private final static String KILDE                       = "KILDE";
    private final static String JSON                        = "JSON";

    private final static String ANDRE_OPPLYSNINGER_TABLE    = "ANDRE_OPPLYSNINGER";
    private final static String ANDRE_OPPLYSNINGER_SEQ      = "ANDRE_OPPLYSNINGER_SEQ";
    private final static String ANDRE_OPPLYSNINGER_ID       = "ANDRE_OPPLYSNINGER_ID";
    private final static String TEKST                       = "TEKST";

    private final JdbcTemplate db;

    @Inject
    public OpplysningerRepository(JdbcTemplate db) {
        this.db = db;
    }

    public long lagOpplysning(Opplysning opplysning) {
        return SqlUtils.insert(db, OPPLYSNING_TABLE)
                .value(OPPLYSNING_ID, nesteFraSekvens(db, OPPLYSNING_SEQ))
                .value(VEDTAK_ID, opplysning.getVedtakId())
                .value(KILDE, opplysning.getOpplysningsType())
                .value(JSON, opplysning.getJson())
                .execute();
    }

    public long lagAndreOpplysninger(AndreOpplysninger andreOpplysninger) {
        return SqlUtils.insert(db, ANDRE_OPPLYSNINGER_TABLE)
                .value(ANDRE_OPPLYSNINGER_ID, nesteFraSekvens(db, ANDRE_OPPLYSNINGER_SEQ))
                .value(VEDTAK_ID, andreOpplysninger.getVedtakId())
                .value(TEKST, andreOpplysninger.getTekst())
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
                .setId(rs.getLong(OPPLYSNING_ID))
                .setVedtakId(rs.getLong(VEDTAK_ID))
                .setOpplysningsType(valueOf(OpplysningsType.class, rs.getString(KILDE)))
                .setJson(rs.getString(JSON));
    }

    public List<AndreOpplysninger> hentAndreOpplysningerForVedtak(long vedtakId) {
        WhereClause where = WhereClause
                .equals(VEDTAK_ID, vedtakId);

        return SqlUtils.select(db, ANDRE_OPPLYSNINGER_TABLE, OpplysningerRepository::mapAndreOpplysninger)
                .where(where)
                .column("*")
                .executeToList();
    }

    @SneakyThrows
    private static AndreOpplysninger mapAndreOpplysninger(ResultSet rs) {
        return new AndreOpplysninger()
                .setId(rs.getLong(ANDRE_OPPLYSNINGER_ID))
                .setVedtakId(rs.getLong(VEDTAK_ID))
                .setTekst(rs.getString(TEKST));
    }
}
