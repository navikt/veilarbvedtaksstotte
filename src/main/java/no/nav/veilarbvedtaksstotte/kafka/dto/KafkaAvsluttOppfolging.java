package no.nav.veilarbvedtaksstotte.kafka.dto;

import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class KafkaAvsluttOppfolging {
   String aktorId;
   ZonedDateTime sluttdato;
}
