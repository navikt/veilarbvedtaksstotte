package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Value;
import java.util.Date;

@Value
public class KafkaAvsluttOppfolging {
   String aktorId;
   Date sluttdato;
}
