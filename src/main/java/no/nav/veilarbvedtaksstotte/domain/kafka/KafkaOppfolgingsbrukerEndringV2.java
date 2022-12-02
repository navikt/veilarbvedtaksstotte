package no.nav.veilarbvedtaksstotte.domain.kafka;


import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Value;

@Value
public class KafkaOppfolgingsbrukerEndringV2 {
    String fodselsnummer;
    String oppfolgingsenhet;
}
