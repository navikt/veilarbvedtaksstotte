package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Value;

@Value
public class PersonNavn {
    String fornavn;
    String mellomnavn;
    String etternavn;
    String sammensattNavn;
}
