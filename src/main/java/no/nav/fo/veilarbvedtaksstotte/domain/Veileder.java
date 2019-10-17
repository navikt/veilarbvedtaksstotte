package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Veileder {
	String ident;
	String navn;
	String fornavn;
	String etternavn;
}
