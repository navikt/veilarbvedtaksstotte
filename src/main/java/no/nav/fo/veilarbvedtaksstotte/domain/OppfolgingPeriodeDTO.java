package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Value;

import java.time.LocalDateTime;

@Value
public class OppfolgingPeriodeDTO {
    public LocalDateTime startDato;
    public LocalDateTime sluttDato;
}
