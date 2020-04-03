package no.nav.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarbvedtaksstotte.domain.enums.SystemMeldingType;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class SystemMelding {
    long id;
    long vedtakId;
    SystemMeldingType systemMeldingType;
    LocalDateTime opprettet;
    String utfortAvNavn;
}
