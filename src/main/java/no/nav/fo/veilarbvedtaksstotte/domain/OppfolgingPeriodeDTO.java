package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OppfolgingPeriodeDTO {
    public LocalDateTime startDato;
    public LocalDateTime sluttDato;
}
