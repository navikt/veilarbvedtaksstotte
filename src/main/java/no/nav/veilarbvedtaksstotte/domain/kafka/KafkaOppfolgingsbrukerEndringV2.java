package no.nav.veilarbvedtaksstotte.domain.kafka;


import lombok.Value;
import no.nav.common.types.identer.Fnr;

@Value
public class KafkaOppfolgingsbrukerEndringV2 {
    Fnr fodselsnummer;
    String oppfolgingsenhet;
}
