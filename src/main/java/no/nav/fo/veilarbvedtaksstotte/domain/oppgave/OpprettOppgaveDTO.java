package no.nav.fo.veilarbvedtaksstotte.domain.oppgave;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class OpprettOppgaveDTO {
	String tildeltEnhetsnr;
	String opprettetAvEnhetsnr;
	String aktoerId;
	String tilordnetRessurs; // beslutter ident
	String beskrivelse;
	String tema;
	String oppgavetype;
	String aktivDato; // required
	String fristFerdigstillelse; // required
	String prioritet; // required
}
