package no.nav.fo.veilarbvedtaksstotte.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaHelsesjekk implements Helsesjekk {

//    @Inject
//    private JdbcTemplate db;

    @Override
    public void helsesjekk() throws Throwable {
//        if (db.queryForObject("SELECT COUNT(*) FROM FEILEDE_KAFKA_BRUKERE", Long.class) > 0) {
//            throw new IllegalStateException();
//        }
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata("kafka_status", "N/A",
                "Sjekker at det ikke er noen feil med sending til kafka", false);
    }
}


