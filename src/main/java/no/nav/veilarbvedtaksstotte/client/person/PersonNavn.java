package no.nav.veilarbvedtaksstotte.client.person;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PersonNavn {
    String fornavn;
    String mellomnavn;
    String etternavn;
    String sammensattNavn;
}
