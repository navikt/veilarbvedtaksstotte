package no.nav.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarbvedtaksstotte.domain.enums.MeldingType;
import no.nav.veilarbvedtaksstotte.domain.enums.MeldingUnderType;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class DialogMeldingDTO {
    String melding;
    LocalDateTime opprettet;
    String opprettetAvIdent;
    String opprettetAvNavn;
    MeldingUnderType meldingUnderType;
    MeldingType meldingType;

    public static DialogMeldingDTO fraMelding(DialogMelding dialogMelding) {
        DialogMeldingDTO melding = new DialogMeldingDTO();

        melding.melding = dialogMelding.melding;
        melding.opprettet = dialogMelding.opprettet;
        melding.opprettetAvIdent = dialogMelding.opprettetAvIdent;
        melding.meldingUnderType = dialogMelding.meldingUnderType;
        melding.meldingType = dialogMelding.meldingType;

        return melding;
    }
}
