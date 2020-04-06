package no.nav.veilarbvedtaksstotte.domain.dialog;

import java.time.LocalDateTime;

public class MeldingDTO {
    LocalDateTime opprettet;
    Type type;

    public enum Type {
        DIALOG_MELDING, SYSTEM_MELDING
    }
}
