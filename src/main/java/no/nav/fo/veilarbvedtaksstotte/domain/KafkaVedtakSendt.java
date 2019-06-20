package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Hovedmal;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;

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
