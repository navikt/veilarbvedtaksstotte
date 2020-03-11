package no.nav.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.domain.OpprettOppgaveDTO;

import javax.inject.Inject;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class OppgaveClient extends BaseClient {

	public static final String OPPGAVE_API_PROPERTY_NAME = "OPPGAVEAPI_URL";
	public static final String OPPGAVE = "oppgave";

	@Inject
	public OppgaveClient() {
		super(getRequiredProperty(OPPGAVE_API_PROPERTY_NAME));
	}

	public void opprettOppgave(OpprettOppgaveDTO opprettOppgaveDTO) {
		post(joinPaths(baseUrl, "api", "v1", "oppgaver"), opprettOppgaveDTO, Void.class)
				.withStatusCheck();
	}

}
