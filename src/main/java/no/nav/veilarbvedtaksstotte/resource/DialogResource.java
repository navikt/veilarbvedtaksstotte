package no.nav.veilarbvedtaksstotte.resource;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.domain.DialogMeldingDTO;
import no.nav.veilarbvedtaksstotte.domain.OpprettDialogMeldingDTO;
import no.nav.veilarbvedtaksstotte.service.DialogService;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.ws.rs.*;
import java.util.List;

@Slf4j
@Controller
@Produces("application/json")
@Path("/{fnr}/dialog")
public class DialogResource {

    private DialogService dialogService;

    @Inject
    public DialogResource(DialogService dialogService) {
        this.dialogService = dialogService;
    }

    @GET
    @Path("/")
    public List<DialogMeldingDTO> hentDialogMeldinger(@PathParam("fnr") String fnr) {
        return dialogService.hentDialogMeldinger(fnr);
    }

    @POST
    @Path("/")
    public void opprettBrukerDialogMelding(@PathParam("fnr") String fnr, OpprettDialogMeldingDTO opprettDialogMeldingDTO) {
        dialogService.opprettBrukerDialogMelding(fnr, opprettDialogMeldingDTO.getMelding());
    }

}
