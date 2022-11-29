package no.nav.veilarbvedtaksstotte.domain.kafka;


import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Value;

@Value
public class KafkaOppfolgingsbrukerEndringV2 {
    @JsonAlias("fodselsnummer")
    String fodselsnummer;
    @JsonAlias("oppfolgingsenhet")
    String oppfolgingsenhet;
}
