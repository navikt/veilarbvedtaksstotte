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
import java.util.List;

@Slf4j
@Controller
@Path("/{fnr}")
public class VedtakResource {

    private VedtakService vedtakService;

    @Inject
    public VedtakResource(VedtakService vedtakService) {
        this.vedtakService = vedtakService;
    }

    @POST
    @Path("/vedtak/send")
    public DokumentSendtDTO sendVedtak(@PathParam("fnr") String fnr) {
        return vedtakService.sendVedtak(fnr);
    }

    @GET
    @Path("/utkast")
    public Vedtak hentUtkast(@PathParam("fnr") String fnr) { return vedtakService.hentUtkast(fnr); }

    @GET
    @Path("/vedtak")
    public List<Vedtak> hentVedtak(@PathParam("fnr") String fnr) { return vedtakService.hentVedtak(fnr); }

    @PUT
    @Path("/vedtak")
    public void upsertVedtak(@PathParam("fnr") String fnr, VedtakDTO vedtakDTO) {
        vedtakService.upsertVedtak(fnr, vedtakDTO);
    }

    @POST
    @Produces("application/pdf")
    @Path("/dokumentutkast")
    public byte[] hentForhandsvisning(@PathParam("fnr") String fnr) {
        return  vedtakService.produserDokumentUtkast(fnr);
    }

    @DELETE
    @Path("/utkast")
    public void deleteUtkast(@PathParam("fnr") String fnr) { vedtakService.slettUtkast(fnr); }


    @POST
    @Path("/kafkatest/{innsatsgruppe}")
    public void kafkaTest(@PathParam("fnr") String fnr, @PathParam("innsatsgruppe")Innsatsgruppe innsatsgruppe) {
        vedtakService.kafkaTest(fnr, innsatsgruppe);
    }

}

