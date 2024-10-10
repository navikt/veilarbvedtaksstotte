package no.nav.veilarbvedtaksstotte.repository;

import no.nav.common.kafka.consumer.feilhandtering.StoredConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRepository;


@Repository
public class KafkaConsumerRecordRepository implements KafkaConsumerRepository {

    public static final String KAFKA_CONSUMER_RECORD_TABLE = "KAFKA_CONSUMER_RECORD";
    private static final String KCR_ID = "ID";
    private static final String TOPIC = "TOPIC";
    private static final String PARTITION = "PARTITION";
    private static final String RECORD_OFFSET = "RECORD_OFFSET";
    private static final String RETRIES = "RETRIES";
    private static final String LAST_RETRY = "LAST_RETRY";
    private static final String KEY = "KEY";
    private static final String VALUE = "VALUE";
    private static final String HEADERS_JSON = "HEADERS_JSON";
    private static final String RECORD_TIMESTAMP = "RECORD_TIMESTAMP";
    private static final String CREATED_AT = "CREATED_AT";

    private final JdbcTemplate db;

    @Autowired
    public KafkaConsumerRecordRepository(JdbcTemplate db, TransactionTemplate transactor) {
        this.db = db;
    }


    public int hentAntallRaderIKafkaConsumerRecordOverRetriesgrense() {
        String sql =
                "SELECT COUNT(*) FROM KAFKA_CONSUMER_RECORD WHERE retries > 4";

        return Optional.ofNullable(
                db.queryForObject(sql, Integer.class)
        ).orElse(0);
    }


    @Override
    public long storeRecord(StoredConsumerRecord record) {
        return 0;
    }

    @Override
    public void deleteRecords(List<Long> ids) {

    }

    @Override
    public boolean hasRecordWithKey(String topic, int partition, byte[] key) {
        return false;
    }

    @Override
    public List<StoredConsumerRecord> getRecords(String topic, int partition, int maxRecords) {
        return List.of();
    }

    @Override
    public void incrementRetries(long id) {

    }

    @Override
    public List<TopicPartition> getTopicPartitions(List<String> topics) {
        return List.of();
    }
}
