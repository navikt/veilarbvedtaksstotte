package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Hovedmal;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.KafkaVedtakStatusType;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class KafkaVedtakStatus {

    long id;

    String brukerFnr;

    String brukerAktorId;

    KafkaVedtakStatusType vedtakStatus;

    Innsatsgruppe innsatsgruppe;

    Hovedmal hovedmal;

    LocalDateTime sistRedigertTidspunkt;

    LocalDateTime statusEndretTidspunkt;

}
