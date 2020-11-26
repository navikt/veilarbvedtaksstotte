package no.nav.veilarbvedtaksstotte.kafka.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Hovedmal;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class KafkaVedtakSendt {
    long id;
    LocalDateTime vedtakSendt;
    Innsatsgruppe innsatsgruppe;
    Hovedmal hovedmal;
    String aktorId;
    String enhetId;
}
