package no.nav.veilarbvedtaksstotte.utils;

import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRepository;
import no.nav.common.kafka.consumer.util.ConsumerUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Serializer;

public class StoreOnFailureArenaTopicConsumer<K, V> implements TopicConsumer<K, V> {

    private final TopicConsumer<K, V> consumer;

    private final KafkaConsumerRepository consumerRepository;

    private final Serializer<K> keySerializer;

    private final Serializer<V> valueSerializer;

    public StoreOnFailureArenaTopicConsumer(
            TopicConsumer<K, V> consumer,
            KafkaConsumerRepository consumerRepository,
            Serializer<K> keySerializer,
            Serializer<V> valueSerializer
    ) {
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
        this.consumer = consumer;
        this.consumerRepository = consumerRepository;
    }

    @Override
    public ConsumeStatus consume(ConsumerRecord<K, V> record) {

        ConsumeStatus status = ConsumerUtils.safeConsume(consumer, record);

        if (status == ConsumeStatus.OK) {
            return ConsumeStatus.OK;
        }

        consumerRepository.storeRecord(ConsumerUtils.mapToStoredRecord(record, keySerializer, valueSerializer));
        return ConsumeStatus.OK;
    }
}
