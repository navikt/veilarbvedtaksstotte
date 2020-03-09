package no.nav.fo.veilarbvedtaksstotte.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Value;

@Value
public class KafkaOppfolgingsbrukerEndring {
   @JsonAlias("aktoerid")
   String aktorId;
   @JsonAlias("nav_kontor")
   String oppfolgingsenhetId;
}
