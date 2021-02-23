package no.nav.veilarbvedtaksstotte.kafka.dto;

import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class KafkaAvsluttOppfolging {
   String aktorId;
   ZonedDateTime sluttdato;

   public String getAktorId() {
      return this.aktorId;
   }

   public ZonedDateTime getSluttdato() {
      return this.sluttdato;
   }
}
