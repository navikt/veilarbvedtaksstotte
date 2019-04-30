package no.nav.fo.veilarbvedtaksstotte.resource;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.domain.DokumentSendtDTO;
import no.nav.fo.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.fo.veilarbvedtaksstotte.domain.VedtakDTO;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.fo.veilarbvedtaksstotte.service.VedtakService;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.ws.rs.*;

@Slf4j
@Controller
@Path("/vedtak")
public class VedtakResource {

    private VedtakService vedtakService;

    @Inject
    public VedtakResource(VedtakService vedtakService) {
        this.vedtakService = vedtakService;
    }

    @POST
    @Path("/send/{fnr}")
    public DokumentSendtDTO sendVedtak(@PathParam("fnr") String fnr) {
        return vedtakService.sendVedtak(fnr);
    }

    @GET
    @Path("/{fnr}")
    public Vedtak hentVedtak(@PathParam("fnr") String fnr) {
        return vedtakService.hentVedtak(fnr);
    }

    @PUT
    @Path("/{fnr}")
    public void upsertVedtak(@PathParam("fnr") String fnr, VedtakDTO vedtakDTO) {
        vedtakService.upsertVedtak(fnr, vedtakDTO);
    }

    @POST
    @Path("/kafkatest/{fnr}/{innsatsgruppe}")
    public void kafkaTest(@PathParam("fnr") String fnr, @PathParam("innsatsgruppe")Innsatsgruppe innsatsgruppe) {
        vedtakService.kafkaTest(fnr, innsatsgruppe);
    }

}

