package no.nav.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarbvedtaksstotte.domain.enums.MeldingType;
import no.nav.veilarbvedtaksstotte.domain.enums.MeldingUnderType;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class DialogMelding {
    long id;
    long vedtakId;
    String melding;
    LocalDateTime opprettet;
    String opprettetAvIdent;
    MeldingUnderType meldingUnderType;
    MeldingType meldingType;
}
