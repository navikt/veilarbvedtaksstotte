package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Value;

import java.util.List;

@Value
public class OppfolgingDTO {

    private String servicegruppe;

    private String formidlingsgruppe;

    private String rettighetsgruppe;

    private List<OppfolgingPeriodeDTO> oppfolgingsPerioder;

}
