package no.nav.veilarbvedtaksstotte.resource;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.domain.OpprettDialogMeldingDTO;
import no.nav.veilarbvedtaksstotte.domain.dialog.MeldingDTO;
import no.nav.veilarbvedtaksstotte.service.MeldingService;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.ws.rs.*;
import java.util.List;

@Slf4j
@Controller
@Produces("application/json")
@Path("/{fnr}/meldinger")
public class MeldingResource {

    private MeldingService meldingService;

    @Inject
    public MeldingResource(MeldingService meldingService) {
        this.meldingService = meldingService;
    }

    @GET
    @Path("/")
    public List<MeldingDTO> hentDialogMeldinger(@PathParam("fnr") String fnr) {
        return meldingService.hentMeldinger(fnr);
    }

    @POST
    @Path("/")
    public void opprettDialogMelding(@PathParam("fnr") String fnr, OpprettDialogMeldingDTO opprettDialogMeldingDTO) {
        meldingService.opprettBrukerDialogMelding(fnr, opprettDialogMeldingDTO.getMelding());
    }

}
