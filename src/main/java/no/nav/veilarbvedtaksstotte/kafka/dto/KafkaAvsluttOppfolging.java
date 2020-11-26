package no.nav.veilarbvedtaksstotte.kafka.dto;

import lombok.Value;
import java.util.Date;

@Value
public class KafkaAvsluttOppfolging {
   String aktorId;
   Date sluttdato;
}
