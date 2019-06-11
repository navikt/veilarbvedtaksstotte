package no.nav.fo.veilarbvedtaksstotte.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ContainerAwareErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class KafkaHelsesjekk implements Helsesjekk, ContainerAwareErrorHandler {
    private long lastThrownExceptionTime;
    private Exception e;

//    @Inject
//    private JdbcTemplate db;

    @Override
    public void helsesjekk() throws Throwable {
//        if (db.queryForObject("SELECT COUNT(*) FROM FEILEDE_KAFKA_BRUKERE", Long.class) > 0) {
//            throw new IllegalStateException();
//        }

        if ((lastThrownExceptionTime + 60_000L) > System.currentTimeMillis()) {
            throw new IllegalArgumentException("Kafka consumer feilet " + new Date(lastThrownExceptionTime), e);
        }
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata("kafka_status", "N/A",
                "Sjekker at det ikke er noen feil med sending til kafka", false);
    }

    @Override
    public void handle(Exception thrownException, List<ConsumerRecord<?, ?>> records, Consumer<?, ?> consumer, MessageListenerContainer container) {
        log.error("Feil i listener:", thrownException);
        lastThrownExceptionTime = System.currentTimeMillis();
        e = thrownException;
    }
}


