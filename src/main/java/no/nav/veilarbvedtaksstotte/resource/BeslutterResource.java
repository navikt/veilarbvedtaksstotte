package no.nav.veilarbvedtaksstotte.resource;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.domain.SendBeslutterOppgaveDTO;
import no.nav.veilarbvedtaksstotte.service.BeslutterOppgaveService;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Slf4j
@Controller
@Produces("application/json")
@Path("/{fnr}/beslutter")
public class BeslutterResource {

	private BeslutterOppgaveService beslutterOppgaveService;

	@Inject
	public BeslutterResource(BeslutterOppgaveService beslutterOppgaveService) {
		this.beslutterOppgaveService = beslutterOppgaveService;
	}

	@POST
	@Path("/send")
	public void sendBeslutterOppgave(@PathParam("fnr") String fnr, SendBeslutterOppgaveDTO sendBeslutterOppgaveDTO) {
		beslutterOppgaveService.sendBeslutterOppgave(sendBeslutterOppgaveDTO, fnr);
	}

}
