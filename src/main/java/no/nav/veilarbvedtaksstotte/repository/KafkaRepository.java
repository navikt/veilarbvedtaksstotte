package no.nav.veilarbvedtaksstotte.repository;

import lombok.SneakyThrows;
import no.nav.veilarbvedtaksstotte.kafka.KafkaTopics;
import no.nav.veilarbvedtaksstotte.repository.domain.FeiletKafkaMelding;
import no.nav.veilarbvedtaksstotte.repository.domain.MeldingType;
import no.nav.veilarbvedtaksstotte.utils.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.List;

import static java.lang.String.format;
import static no.nav.veilarbvedtaksstotte.utils.EnumUtils.getName;

@Repository
public class KafkaRepository {

    public final static String FEILET_KAFKA_MELDING_TABLE               = "FEILET_KAFKA_MELDING";
    private final static String ID                                      = "ID";
    private final static String TOPIC                                   = "TOPIC";
    private final static String KEY                                     = "KEY";
    private final static String PAYLOAD                                 = "PAYLOAD";
    private final static String MESSAGE_TYPE                            = "MESSAGE_TYPE";
    private final static String MESSAGE_OFFSET                          = "MESSAGE_OFFSET";

    private final JdbcTemplate db;

    @Autowired
    public KafkaRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void lagreFeiletProdusertKafkaMelding(KafkaTopics.Topic topic, String key, String jsonPayload) {
        String sql = format(
                "INSERT INTO %s (%s, %s, %s, %s) VALUES (?, ?, ?::json, ?::KAFKA_MESSAGE_TYPE)",
                FEILET_KAFKA_MELDING_TABLE, TOPIC, KEY, PAYLOAD, MESSAGE_TYPE
        );

        db.update(sql, getName(topic), key, jsonPayload, getName(MeldingType.PRODUCED));
    }

    public void lagreFeiletKonsumertKafkaMelding(KafkaTopics.Topic topic, String key, String jsonPayload, long offset) {
        String sql = format(
                "INSERT INTO %s (%s, %s, %s, %s, %s) VALUES (?, ?, ?::json, ?::KAFKA_MESSAGE_TYPE, ?)",
                FEILET_KAFKA_MELDING_TABLE, TOPIC, KEY, PAYLOAD, MESSAGE_TYPE, MESSAGE_OFFSET
        );

        db.update(sql, getName(topic), key, jsonPayload, getName(MeldingType.CONSUMED), offset);
    }

    public List<FeiletKafkaMelding> hentFeiledeKafkaMeldinger(KafkaTopics.Topic topic, MeldingType type) {
        String sql = format(
                "SELECT * FROM %s WHERE %s = ? AND %s = ?::KAFKA_MESSAGE_TYPE",
                FEILET_KAFKA_MELDING_TABLE, TOPIC, MESSAGE_TYPE
        );

        return db.query(sql, new Object[]{getName(topic), getName(type)}, KafkaRepository::mapFeiletKafkaMelding);
    }

    public void slettFeiletKafkaMelding(long feiletMeldingId) {
        db.update(format("DELETE FROM %S WHERE %s = ?", FEILET_KAFKA_MELDING_TABLE, ID), feiletMeldingId);
    }

    @SneakyThrows
    private static FeiletKafkaMelding mapFeiletKafkaMelding(ResultSet rs, int rowNum) {
        return new FeiletKafkaMelding()
                .setId(rs.getLong(ID))
                .setTopic(EnumUtils.valueOf(KafkaTopics.Topic.class, rs.getString(TOPIC)))
                .setKey(rs.getString(KEY))
                .setType(EnumUtils.valueOf(MeldingType.class, rs.getString(MESSAGE_TYPE)))
                .setOffset(rs.getLong(MESSAGE_OFFSET))
                .setJsonPayload(rs.getString(PAYLOAD));
    }

}
