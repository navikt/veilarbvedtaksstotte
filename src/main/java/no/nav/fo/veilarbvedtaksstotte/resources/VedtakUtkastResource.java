package no.nav.fo.veilarbvedtaksstotte.resources;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbvedtaksstotte.db.VedtaksstotteRepository;
import no.nav.fo.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.fo.veilarbvedtaksstotte.domain.VedtakDTO;
import no.nav.fo.veilarbvedtaksstotte.services.VeilederService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;

import static no.bekk.bekkopen.person.FodselsnummerValidator.isValid;

@Component
@Slf4j
@Path("/vedtak")
public class VedtakUtkastResource {

    private VedtaksstotteRepository vedtaksstotteRepository;
    private PepClient pepClient;
    private AktorService aktorService;
    private VeilederService veilederService;

    @Inject
    public VedtakUtkastResource(VedtaksstotteRepository vedtaksstotteRepository,
                                PepClient pepClient,
                                AktorService aktorService,
                                VeilederService veilederService) {
        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.pepClient = pepClient;
        this.aktorService = aktorService;
        this.veilederService = veilederService;
    }

    @GET
    @Path("/{fnr}")
    public Vedtak hentVedtak(@PathParam("fnr") final String fnr) {

        if (fnr == null || !isValid(fnr)) {
            throw new BadRequestException("Fnr mangler eller er ugyldig");
        }

        pepClient.sjekkLeseTilgangTilFnr(fnr);

        String aktorId = aktorService.getAktorId(fnr).orElseThrow(() ->
                new IllegalArgumentException("Fant ikke aktør for fnr: " + fnr));

        return vedtaksstotteRepository.hentVedtakUtkast(aktorId);
    }

    @PUT
    @Path("/{fnr}")
    public void upsertVedtak(@PathParam("fnr") final String fnr, VedtakDTO vedtakDTO) {

        if (fnr == null || !isValid(fnr)) {
            throw new BadRequestException("Fnr mangler eller er ugyldig");
        }

        pepClient.sjekkLeseTilgangTilFnr(fnr);

        String aktorId = aktorService.getAktorId(fnr).orElseThrow(() ->
                        new IllegalArgumentException("Fant ikke aktør for fnr: " + fnr));

        Vedtak vedtak = vedtakDTO.tilVedtak()
                .setVeileder(veilederService.hentVeilederFraToken());

        vedtaksstotteRepository.upsertUtkast(aktorId, vedtak);

    }

}
