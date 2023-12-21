package no.nav.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.Oyeblikksbilde;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeType;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.dto.OyeblikksbildeInputDto;
import no.nav.veilarbvedtaksstotte.utils.DbUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static no.nav.veilarbvedtaksstotte.utils.EnumUtils.getName;
import static no.nav.veilarbvedtaksstotte.utils.EnumUtils.valueOf;

@Repository
@Slf4j
public class OyeblikksbildeRepository {

    public final static String OYEBLIKKSBILDE_TABLE = "OYEBLIKKSBILDE";
    private final static String VEDTAK_ID = "VEDTAK_ID";
    private final static String OYEBLIKKSBILDE_TYPE = "OYEBLIKKSBILDE_TYPE";
    private final static String JSON = "JSON";
    private final static String DOKUMENT_ID = "DOKUMENT_ID";

    private final JdbcTemplate db;

    @Autowired
    public OyeblikksbildeRepository(JdbcTemplate db) {
        this.db = db;
    }

    public List<Oyeblikksbilde> hentOyeblikksbildeForVedtak(long vedtakId) {
        String sql = format("SELECT * FROM %s WHERE %s = ?", OYEBLIKKSBILDE_TABLE, VEDTAK_ID);
        return db.query(sql, OyeblikksbildeRepository::mapOyeblikksbilde, vedtakId);
    }

    public void slettOyeblikksbilder(long vedtakId) {
        db.update(format("DELETE FROM %s WHERE %s = %d", OYEBLIKKSBILDE_TABLE, VEDTAK_ID, vedtakId));
    }

    public void upsertOyeblikksbilde(OyeblikksbildeInputDto oyeblikksbilde) {
        long vedtakId = oyeblikksbilde.getVedtakId();
        String type = getName(oyeblikksbilde.getOyeblikksbildeType());
        String json = oyeblikksbilde.getJson();

        boolean harOyblikksbilde = hentOyeblikksbilde(vedtakId, oyeblikksbilde.getOyeblikksbildeType()).isPresent();

        // Dette kunne også blitt løst med upsert hvis vi hadde laget en composite index på id og type
        if (harOyblikksbilde) {
            db.update(
                    "UPDATE OYEBLIKKSBILDE SET JSON = ?::json WHERE VEDTAK_ID = ? AND OYEBLIKKSBILDE_TYPE = ?::OYEBLIKKSBILDE_TYPE",
                    json, vedtakId, type
            );
        } else {
            db.update(
                    "INSERT INTO OYEBLIKKSBILDE (VEDTAK_ID, OYEBLIKKSBILDE_TYPE, JSON) VALUES (?,?::OYEBLIKKSBILDE_TYPE,?::json)",
                    vedtakId, type, json
            );
        }
    }

    private Optional<Oyeblikksbilde> hentOyeblikksbilde(long vedtakId, OyeblikksbildeType type) {
        String sql = format("SELECT * FROM %s WHERE %s = ? AND %s = ?::OYEBLIKKSBILDE_TYPE LIMIT 1", OYEBLIKKSBILDE_TABLE, VEDTAK_ID, OYEBLIKKSBILDE_TYPE);
        return Optional.ofNullable(DbUtils.queryForObjectOrNull(
                () -> db.queryForObject(sql, OyeblikksbildeRepository::mapOyeblikksbilde, vedtakId, getName(type)))
        );
    }

    public void lagreJournalfortDokumentId(long vedtakId, String dokumentId, OyeblikksbildeType oyeblikksbildeType) {
        db.update(
                "UPDATE OYEBLIKKSBILDE SET dokument_id = ?  WHERE VEDTAK_ID = ? AND OYEBLIKKSBILDE_TYPE = ?::OYEBLIKKSBILDE_TYPE",
                dokumentId, vedtakId, oyeblikksbildeType.name()
        );
    }

    @SneakyThrows
    private static Oyeblikksbilde mapOyeblikksbilde(ResultSet rs, int row) {
        String ingenData = "{\"ingenData\": \"Ingen registrerte data\"}";
        boolean harIngenData = rs.getString(JSON) == null || rs.getString(JSON).length() < 100;
        return new Oyeblikksbilde()
                .setVedtakId(rs.getLong(VEDTAK_ID))
                .setOyeblikksbildeType(valueOf(OyeblikksbildeType.class, rs.getString(OYEBLIKKSBILDE_TYPE)))
                .setJson(harIngenData ? ingenData : rs.getString(JSON))
                .setJournalfort(rs.getString(DOKUMENT_ID) != null && !rs.getString(DOKUMENT_ID).isEmpty());
    }

    public String hentJournalfortDokumentId(long vedtakId, OyeblikksbildeType oyeblikksbildeType) {
        String sql = format("SELECT dokument_id FROM %s WHERE %s = ? AND OYEBLIKKSBILDE_TYPE = ?::OYEBLIKKSBILDE_TYPE", OYEBLIKKSBILDE_TABLE, VEDTAK_ID);
        return db.queryForObject(sql, String.class, vedtakId, oyeblikksbildeType.name());
    }
}

