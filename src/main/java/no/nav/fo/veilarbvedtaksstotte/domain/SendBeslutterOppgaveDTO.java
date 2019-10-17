package no.nav.fo.veilarbvedtaksstotte.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SendBeslutterOppgaveDTO {
	String aktivFra;        // må være på format YYYY-MM-DD
	String frist;           // må være null eller på format YYYY-MM-DD
	String enhetId;         // må være gyldig enhet id
	String beslutterIdent;  // må være gyldig ident eller null
	String beslutterNavn;   // fritekst, kan være null
	String prioritet;       // LAV, NORM, HOY
	String beskrivelse;     // fritekst, kan være null
}
