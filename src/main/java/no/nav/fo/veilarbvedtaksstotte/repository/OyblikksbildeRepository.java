package no.nav.fo.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.fo.veilarbvedtaksstotte.domain.Oyblikksbilde;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.OyblikksbildeType;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.List;

import static no.nav.fo.veilarbvedtaksstotte.utils.EnumUtils.getName;
import static no.nav.fo.veilarbvedtaksstotte.utils.EnumUtils.valueOf;

@Repository
public class OyblikksbildeRepository {

    public final static String OYBLIKKSBILDE_TABLE          = "OYBLIKKSBILDE";
    private final static String VEDTAK_ID                   = "VEDTAK_ID";
    private final static String OYBLIKKSBILDE_TYPE          = "OYBLIKKSBILDE_TYPE";
    private final static String JSON                        = "JSON";

    private final JdbcTemplate db;

    @Inject
    public OyblikksbildeRepository(JdbcTemplate db) {
        this.db = db;
    }

    public List<Oyblikksbilde> hentOyblikksbildeForVedtak(long vedtakId) {
        return SqlUtils.select(db, OYBLIKKSBILDE_TABLE, OyblikksbildeRepository::mapOyblikksbilde)
                .where(WhereClause.equals(VEDTAK_ID, vedtakId))
                .column("*")
                .executeToList();
    }

    public void lagOyblikksbilde(List<Oyblikksbilde> oyblikksbildeList) {
        oyblikksbildeList.forEach(this::lagOyblikksbilde);
    }

    private void lagOyblikksbilde(Oyblikksbilde oyblikksbilde) {
        db.update(
        "INSERT INTO OYBLIKKSBILDE (VEDTAK_ID, OYBLIKKSBILDE_TYPE, JSON) VALUES (?,?::OYBLIKKSBILDE_TYPE,?::json)",
            oyblikksbilde.getVedtakId(), getName(oyblikksbilde.getOyblikksbildeType()), oyblikksbilde.getJson()
        );
    }

    @SneakyThrows
    private static Oyblikksbilde mapOyblikksbilde(ResultSet rs) {
        return new Oyblikksbilde()
                .setVedtakId(rs.getLong(VEDTAK_ID))
                .setOyblikksbildeType(valueOf(OyblikksbildeType.class, rs.getString(OYBLIKKSBILDE_TYPE)))
                .setJson(rs.getString(JSON));
    }

}

