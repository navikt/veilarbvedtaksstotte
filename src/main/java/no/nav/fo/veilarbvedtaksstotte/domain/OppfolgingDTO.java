package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Value;

import java.util.List;

@Value
public class OppfolgingDTO {
    private List<OppfolgingPeriodeDTO> oppfolgingsPerioder;
}
