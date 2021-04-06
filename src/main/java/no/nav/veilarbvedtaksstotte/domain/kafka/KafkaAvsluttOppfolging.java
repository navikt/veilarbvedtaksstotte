package no.nav.veilarbvedtaksstotte.domain.kafka;

import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class KafkaAvsluttOppfolging {
   String aktorId;
   ZonedDateTime sluttdato;
}
