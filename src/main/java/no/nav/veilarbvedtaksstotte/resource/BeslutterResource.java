package no.nav.veilarbvedtaksstotte.resource;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.service.BeslutterService;
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

	private BeslutterService beslutterService;

	@Inject
	public BeslutterResource(BeslutterService beslutterService) {
		this.beslutterService = beslutterService;
	}

	@POST
	@Path("/start")
	public void startBeslutterProsess(@PathParam("fnr") String fnr) {
		beslutterService.startBeslutterProsess(fnr);
	}

}
