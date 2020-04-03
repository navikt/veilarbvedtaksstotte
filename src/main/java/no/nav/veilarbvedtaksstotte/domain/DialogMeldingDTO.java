package no.nav.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class DialogMeldingDTO {
    String melding;
    LocalDateTime opprettet;
    String opprettetAvIdent;
    String opprettetAvNavn;

    public static DialogMeldingDTO fraMelding(DialogMelding dialogMelding) {
        DialogMeldingDTO melding = new DialogMeldingDTO();

        melding.melding = dialogMelding.melding;
        melding.opprettet = dialogMelding.opprettet;
        melding.opprettetAvIdent = dialogMelding.opprettetAvIdent;

        return melding;
    }

    public static DialogMeldingDTO fraSystemMelding(SystemMelding systemMelding) {
        DialogMeldingDTO melding = new DialogMeldingDTO();

        melding.melding = String.valueOf(systemMelding.systemMeldingType);
        melding.opprettet = systemMelding.opprettet;
        melding.opprettetAvIdent = null;
        melding.opprettetAvNavn = systemMelding.utfortAvNavn;

        return melding;
    }
}
