package no.nav.veilarbvedtaksstotte.domain.kafka;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Value;

@Value
public class KafkaOppfolgingsbrukerEndring {
   @JsonAlias("aktoerid")
   String aktorId;
   @JsonAlias("nav_kontor")
   String oppfolgingsenhetId;
}
