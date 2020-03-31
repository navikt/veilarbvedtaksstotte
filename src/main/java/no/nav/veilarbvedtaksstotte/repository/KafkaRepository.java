package no.nav.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import no.nav.veilarbvedtaksstotte.domain.FeiletKafkaMelding;
import no.nav.veilarbvedtaksstotte.domain.enums.KafkaTopic;
import no.nav.veilarbvedtaksstotte.utils.EnumUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.util.List;

import static no.nav.veilarbvedtaksstotte.utils.EnumUtils.getName;

@Repository
public class KafkaRepository {

    public final static String FEILET_KAFKA_MELDING_TABLE               = "FEILET_KAFKA_MELDING";
    private final static String ID                                      = "ID";
    private final static String TOPIC                                   = "TOPIC";
    private final static String KEY                                     = "KEY";
    private final static String PAYLOAD                                 = "PAYLOAD";

    private final JdbcTemplate db;

    @Inject
    public KafkaRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void lagreFeiletKafkaMelding(KafkaTopic topic, String key, String jsonPayload) {
        String sql = String.format(
                "INSERT INTO %s (%s, %s, %s) VALUES (?::KAFKA_TOPIC_TYPE, ?, ?::json)",
                FEILET_KAFKA_MELDING_TABLE, TOPIC, KEY, PAYLOAD
        );

        db.update(sql, getName(topic), key, jsonPayload);
    }

    public List<FeiletKafkaMelding> hentFeiledeKafkaMeldinger(KafkaTopic topic) {
        String sql = String.format(
                "SELECT * FROM %s WHERE %s = ?::KAFKA_TOPIC_TYPE",
                FEILET_KAFKA_MELDING_TABLE, TOPIC
        );

        return db.query(sql, new Object[]{getName(topic)}, KafkaRepository::mapFeiletKafkaMelding);
    }

    public void slettFeiletKafkaMelding(long feiletMeldingId) {
        SqlUtils.delete(db, FEILET_KAFKA_MELDING_TABLE)
                .where(WhereClause.equals(ID, feiletMeldingId))
                .execute();
    }

    @SneakyThrows
    private static FeiletKafkaMelding mapFeiletKafkaMelding(ResultSet rs, int rowNum) {
        return new FeiletKafkaMelding()
                .setId(rs.getLong(ID))
                .setTopic(EnumUtils.valueOf(KafkaTopic.class, rs.getString(TOPIC)))
                .setKey(rs.getString(KEY))
                .setJsonPayload(rs.getString(PAYLOAD));
    }

}
