package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class KafkaVedtakSendt {
    Timestamp vedtakSendt;
    Innsatsgruppe innsatsgruppe;
    String aktorId;
    String enhetId;
}
