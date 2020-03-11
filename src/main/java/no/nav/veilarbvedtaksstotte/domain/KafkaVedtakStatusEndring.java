package no.nav.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarbvedtaksstotte.domain.enums.Hovedmal;
import no.nav.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.domain.enums.KafkaVedtakStatus;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class KafkaVedtakStatusEndring {

    long vedtakId;

    String aktorId;

    KafkaVedtakStatus vedtakStatus;

    Innsatsgruppe innsatsgruppe;

    Hovedmal hovedmal;

    LocalDateTime statusEndretTidspunkt;

}
