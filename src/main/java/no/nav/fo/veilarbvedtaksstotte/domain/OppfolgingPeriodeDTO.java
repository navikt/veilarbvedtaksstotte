package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Value;

import java.util.Date;

@Value
public class OppfolgingPeriodeDTO {
    public Date startDato;
    public Date sluttDato;
}
