package no.nav.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.Oyeblikksbilde;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.List;

import static java.lang.String.format;
import static no.nav.veilarbvedtaksstotte.utils.EnumUtils.getName;
import static no.nav.veilarbvedtaksstotte.utils.EnumUtils.valueOf;

@Repository
public class OyeblikksbildeRepository {

    public final static String OYEBLIKKSBILDE_TABLE         = "OYEBLIKKSBILDE";
    private final static String VEDTAK_ID                   = "VEDTAK_ID";
    private final static String OYEBLIKKSBILDE_TYPE         = "OYEBLIKKSBILDE_TYPE";
    private final static String JSON                        = "JSON";

    private final JdbcTemplate db;

    @Autowired
    public OyeblikksbildeRepository(JdbcTemplate db) {
        this.db = db;
    }

    public List<Oyeblikksbilde> hentOyeblikksbildeForVedtak(long vedtakId) {
        String sql = format("SELECT * FROM %s WHERE %s = ?", OYEBLIKKSBILDE_TABLE, VEDTAK_ID);
        return db.query(sql, new Object[]{vedtakId}, OyeblikksbildeRepository::mapOyeblikksbilde);
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
    private static Oyeblikksbilde mapOyeblikksbilde(ResultSet rs, int row) {
        return new Oyeblikksbilde()
                .setVedtakId(rs.getLong(VEDTAK_ID))
                .setOyeblikksbildeType(valueOf(OyeblikksbildeType.class, rs.getString(OYEBLIKKSBILDE_TYPE)))
                .setJson(rs.getString(JSON));
    }

}

