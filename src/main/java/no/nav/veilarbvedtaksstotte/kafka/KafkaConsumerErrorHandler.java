package no.nav.veilarbvedtaksstotte.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ErrorHandler;

public class KafkaConsumerErrorHandler implements ErrorHandler {

    @Override
    public void handle(Exception thrownException, ConsumerRecord<?, ?> data) {

    }

    @Override
    public void handle(Exception thrownException, ConsumerRecord<?, ?> data, Consumer<?, ?> consumer) {

    }

}
