package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@ToString
@EqualsAndHashCode
public class Veileder {
    String ident;
    String enhetId;
    String enhetNavn;
}
