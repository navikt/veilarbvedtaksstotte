package no.nav.veilarbvedtaksstotte.domain.dialog;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DialogMelding extends Melding {
    String melding;
    String opprettetAvIdent;
}
