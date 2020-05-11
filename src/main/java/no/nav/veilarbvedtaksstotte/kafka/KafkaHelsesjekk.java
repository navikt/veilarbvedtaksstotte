package no.nav.veilarbvedtaksstotte.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.listener.ContainerAwareErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.List;

@Profile("!local")
@Slf4j
@Component
public class KafkaHelsesjekk implements ContainerAwareErrorHandler {
    private long lastThrownExceptionTime;
    private static final long EN_MINUTT = 60_000L;
    private Exception e;

//    @Override
//    public void helsesjekk() throws Throwable {
//        if ((lastThrownExceptionTime + EN_MINUTT) > System.currentTimeMillis()) {
//            throw new IllegalArgumentException("Kafka consumer feilet " + new Date(lastThrownExceptionTime), e);
//        }
//    }
//
//    @Override
//    public HelsesjekkMetadata getMetadata() {
//        return new HelsesjekkMetadata("kafka_status", "N/A",
//                "Sjekker at det ikke er noen feil med sending til kafka", false);
//    }

    @Override
    public void handle(Exception thrownException, List<ConsumerRecord<?, ?>> records, Consumer<?, ?> consumer, MessageListenerContainer container) {
        log.error("Feil i listener:", thrownException);
        lastThrownExceptionTime = System.currentTimeMillis();
        e = thrownException;
    }
}


