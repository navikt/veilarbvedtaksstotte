package no.nav.veilarbvedtaksstotte.client.veilederogenhet;

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
