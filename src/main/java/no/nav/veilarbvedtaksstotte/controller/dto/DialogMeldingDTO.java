package no.nav.veilarbvedtaksstotte.controller.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarbvedtaksstotte.domain.dialog.DialogMelding;

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

        melding.melding = dialogMelding.getMelding();
        melding.opprettet = dialogMelding.getOpprettet();
        melding.opprettetAvIdent = dialogMelding.getOpprettetAvIdent();

        return melding;
    }
}
