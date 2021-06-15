package no.nav.veilarbvedtaksstotte.utils;

import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRepository;
import no.nav.common.kafka.consumer.util.ConsumerUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class StoreOnFailureArenaTopicConsumer implements TopicConsumer<byte[], byte[]> {

    private final TopicConsumer<byte[], byte[]> consumer;

    private final KafkaConsumerRepository consumerRepository;

    public StoreOnFailureArenaTopicConsumer(
            TopicConsumer<byte[], byte[]> consumer,
            KafkaConsumerRepository consumerRepository
    ) {
        this.consumer = consumer;
        this.consumerRepository = consumerRepository;
    }

    @Override
    public ConsumeStatus consume(ConsumerRecord<byte[], byte[]> record) {

        ConsumeStatus status = ConsumerUtils.safeConsume(consumer, record);

        if (status == ConsumeStatus.OK) {
            return ConsumeStatus.OK;
        }

        consumerRepository.storeRecord(ConsumerUtils.mapToStoredRecord(record));
        return ConsumeStatus.OK;
    }
}
