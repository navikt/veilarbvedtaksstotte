package no.nav.veilarbvedtaksstotte.resource;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.domain.*;
import no.nav.veilarbvedtaksstotte.service.ArenaVedtakService;
import no.nav.veilarbvedtaksstotte.service.OyeblikksbildeService;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

@Slf4j
@Controller
@Produces("application/json")
@Path("/{fnr}")
public class VedtakResource {

    private VedtakService vedtakService;
    private ArenaVedtakService arenaVedtakService;
    private OyeblikksbildeService oyeblikksbildeService;

    @Inject
    public VedtakResource(VedtakService vedtakService, ArenaVedtakService arenaVedtakService, OyeblikksbildeService oyeblikksbildeService) {
        this.vedtakService = vedtakService;
        this.arenaVedtakService = arenaVedtakService;
        this.oyeblikksbildeService = oyeblikksbildeService;
    }

    @POST
    @Path("/vedtak/send")
    public DokumentSendtDTO sendVedtak(@PathParam("fnr") String fnr) {
        return vedtakService.sendVedtak(fnr);
    }

    @GET
    @Produces("application/pdf")
    @Path("/vedtak/pdf")
    public Response hentVedtakPdf(@PathParam("fnr") String fnr,
                                  @QueryParam("dokumentInfoId") String dokumentInfoId,
                                  @QueryParam("journalpostId") String journalpostId) {
        byte[] vedtakPdf = vedtakService.hentVedtakPdf(fnr, dokumentInfoId, journalpostId);
        return Response.ok(vedtakPdf)
                .header("Content-Disposition",  "filename=vedtaksbrev.pdf")
                .build();
    }

    @GET
    @Path("/vedtak")
    public List<Vedtak> hentVedtak(@PathParam("fnr") String fnr) {
        return vedtakService.hentVedtak(fnr);
    }

    @GET
    @Path("/vedtakFraArena")
    public List<ArkivertVedtak> hentVedtakFraArena(@PathParam("fnr") String fnr) {
        return arenaVedtakService.hentVedtakFraArena(fnr);
    }

    @POST
    @Path("/utkast")
    public void lagUtkast(@PathParam("fnr") String fnr) {
        vedtakService.lagUtkast(fnr);
    }

    @PUT
    @Path("/utkast")
    public void oppdaterUtkast(@PathParam("fnr") String fnr, VedtakDTO vedtakDTO) {
        vedtakService.oppdaterUtkast(fnr, vedtakDTO);
    }

    @GET
    @Path("/harUtkast")
    public boolean harUtkast(@PathParam("fnr") String fnr) {
       return vedtakService.harUtkast(fnr);
    }

    @GET
    @Produces("application/pdf")
    @Path("/utkast/pdf")
    public Response hentForhandsvisning(@PathParam("fnr") String fnr) {
        byte[] utkastPdf = vedtakService.produserDokumentUtkast(fnr);
        return Response.ok(utkastPdf)
                .header("Content-Disposition", "filename=vedtaksbrev-utkast.pdf")
                .build();
    }

    @DELETE
    @Path("/utkast")
    public void deleteUtkast(@PathParam("fnr") String fnr) { vedtakService.slettUtkastForFnr(fnr); }

    @GET
    @Path("/oyeblikksbilde/{vedtakid}")
    public List<Oyeblikksbilde> hentOyeblikksbilde(@PathParam("fnr") String fnr, @PathParam("vedtakid") long vedtakId) {
        return oyeblikksbildeService.hentOyeblikksbildeForVedtak(fnr, vedtakId);
    }

    @POST
    @Path("/utkast/overta")
    public void oppdaterUtkast(@PathParam("fnr") String fnr) {
        vedtakService.taOverUtkast(fnr);
    }
}

