package no.nav.veilarbvedtaksstotte.controller.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MeldingDTO {
    LocalDateTime opprettet;
    MeldingType type;

    public enum MeldingType {
        DIALOG_MELDING, SYSTEM_MELDING
    }
}
