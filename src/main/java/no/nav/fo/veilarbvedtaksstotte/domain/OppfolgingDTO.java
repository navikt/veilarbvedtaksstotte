package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Data;

import java.util.List;

@Data
public class OppfolgingDTO {

    private String servicegruppe;

    private String formidlingsgruppe;

    private String rettighetsgruppe;

    private List<OppfolgingPeriodeDTO> oppfolgingsPerioder;

}
