package no.nav.veilarbvedtaksstotte.domain.dialog;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DialogMeldingDTO extends MeldingDTO {
    String melding;
    String opprettetAvIdent;
    String opprettetAvNavn;

    public DialogMeldingDTO() {
        type = MeldingType.DIALOG_MELDING;
    }

    public static DialogMeldingDTO fraMelding(DialogMelding dialogMelding) {
        DialogMeldingDTO melding = new DialogMeldingDTO();

        melding.melding = dialogMelding.melding;
        melding.opprettet = dialogMelding.opprettet;
        melding.opprettetAvIdent = dialogMelding.opprettetAvIdent;

        return melding;
    }
}
