package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Value;

import java.time.LocalDate;

@Value
public class OppfolgingPeriodeDTO {
    public LocalDate startDato;
    public LocalDate sluttDato;
}
