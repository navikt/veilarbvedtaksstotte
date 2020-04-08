package no.nav.veilarbvedtaksstotte.resource;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.domain.BeslutteroversiktSok;
import no.nav.veilarbvedtaksstotte.domain.BrukereMedAntall;
import no.nav.veilarbvedtaksstotte.service.BeslutteroversiktService;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Slf4j
@Controller
@Produces("application/json")
@Path("/beslutteroversikt")
public class BeslutteroversiktResource {

    private BeslutteroversiktService beslutteroversiktService;

    @Inject
    public BeslutteroversiktResource(BeslutteroversiktService beslutteroversiktService) {
        this.beslutteroversiktService = beslutteroversiktService;
    }

    @POST
    @Path("/sok")
    public BrukereMedAntall startBeslutterProsess(BeslutteroversiktSok sokData) {
        return beslutteroversiktService.sokEtterBruker(sokData);
    }

}
