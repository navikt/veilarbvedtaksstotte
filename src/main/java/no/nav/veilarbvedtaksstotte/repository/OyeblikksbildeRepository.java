package no.nav.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.veilarbvedtaksstotte.domain.Oyeblikksbilde;
import no.nav.veilarbvedtaksstotte.domain.enums.OyeblikksbildeType;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.List;

import static no.nav.veilarbvedtaksstotte.utils.EnumUtils.getName;
import static no.nav.veilarbvedtaksstotte.utils.EnumUtils.valueOf;

@Repository
public class OyeblikksbildeRepository {

    public final static String OYEBLIKKSBILDE_TABLE         = "OYEBLIKKSBILDE";
    private final static String VEDTAK_ID                   = "VEDTAK_ID";
    private final static String OYEBLIKKSBILDE_TYPE         = "OYEBLIKKSBILDE_TYPE";
    private final static String JSON                        = "JSON";

    private final JdbcTemplate db;

    @Inject
    public OyeblikksbildeRepository(JdbcTemplate db) {
        this.db = db;
    }

    public List<Oyeblikksbilde> hentOyeblikksbildeForVedtak(long vedtakId) {
        return SqlUtils.select(db, OYEBLIKKSBILDE_TABLE, OyeblikksbildeRepository::mapOyeblikksbilde)
                .where(WhereClause.equals(VEDTAK_ID, vedtakId))
                .column("*")
                .executeToList();
    }

    public void lagOyeblikksbilde(List<Oyeblikksbilde> oyeblikksbildeList) {
        oyeblikksbildeList.forEach(this::lagOyeblikksbilde);
    }

    private void lagOyeblikksbilde(Oyeblikksbilde oyeblikksbilde) {
        db.update(
        "INSERT INTO OYEBLIKKSBILDE (VEDTAK_ID, OYEBLIKKSBILDE_TYPE, JSON) VALUES (?,?::OYEBLIKKSBILDE_TYPE,?::json)",
            oyeblikksbilde.getVedtakId(), getName(oyeblikksbilde.getOyeblikksbildeType()), oyeblikksbilde.getJson()
        );
    }

    @SneakyThrows
    private static Oyeblikksbilde mapOyeblikksbilde(ResultSet rs) {
        return new Oyeblikksbilde()
                .setVedtakId(rs.getLong(VEDTAK_ID))
                .setOyeblikksbildeType(valueOf(OyeblikksbildeType.class, rs.getString(OYEBLIKKSBILDE_TYPE)))
                .setJson(rs.getString(JSON));
    }

}

