package no.nav.veilarbvedtaksstotte.utils;

import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRepository;
import no.nav.common.kafka.consumer.util.ConsumerUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class StoreOnFailureArenaTopicConsumer implements TopicConsumer<byte[], byte[]> {

    private final TopicConsumer<byte[], byte[]> topicConsumer;

    private final KafkaConsumerRepository consumerRepository;

    public StoreOnFailureArenaTopicConsumer(
            TopicConsumer<byte[], byte[]> topicConsumer,
            KafkaConsumerRepository consumerRepository
    ) {
        this.topicConsumer = topicConsumer;
        this.consumerRepository = consumerRepository;
    }

    @Override
    public ConsumeStatus consume(ConsumerRecord<byte[], byte[]> consumerRecord) {

        ConsumeStatus status = ConsumerUtils.safeConsume(topicConsumer, consumerRecord);

        if (status == ConsumeStatus.OK) {
            return ConsumeStatus.OK;
        }

        consumerRepository.storeRecord(ConsumerUtils.mapToStoredRecord(consumerRecord));
        return ConsumeStatus.OK;
    }
}
