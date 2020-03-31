package no.nav.veilarbvedtaksstotte.resource;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.domain.BeslutteroversiktBruker;
import no.nav.veilarbvedtaksstotte.domain.BeslutteroversiktSok;
import no.nav.veilarbvedtaksstotte.service.BeslutteroversiktService;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.List;

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
    public List<BeslutteroversiktBruker> startBeslutterProsess(BeslutteroversiktSok sokData) {
        return beslutteroversiktService.sokEtterBruker(sokData);
    }

}
