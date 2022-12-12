package no.nav.veilarbvedtaksstotte.domain.kafka;


import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Value;
import no.nav.common.types.identer.Fnr;

@Value
public class KafkaOppfolgingsbrukerEndringV2 {
    Fnr fodselsnummer;
    String oppfolgingsenhet;
}
