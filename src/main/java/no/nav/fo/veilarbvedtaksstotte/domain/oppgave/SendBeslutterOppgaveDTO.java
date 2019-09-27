package no.nav.fo.veilarbvedtaksstotte.domain.oppgave;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SendBeslutterOppgaveDTO {
	String aktivFra;
	String frist;
	String enhet;
	String beslutter;
	String beskrivelse;
}
