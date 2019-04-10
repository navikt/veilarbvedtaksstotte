package no.nav.fo.veilarbvedtaksstotte.resources;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import static no.bekk.bekkopen.person.FodselsnummerValidator.isValid;

@Component
@Path("/vedtak")
@Slf4j
public class VedtakResource {

    private PepClient pepClient;
    private AktorService aktorService;

    @Inject
    public VedtakResource(PepClient pepClient, AktorService aktorService) {
        this.pepClient = pepClient;
        this.aktorService = aktorService;
    }

    @GET
    @Path("/")
    public String hentVedtakUtkast(@QueryParam("fnr") final String fnr) {

        if (fnr == null || !isValid(fnr)) {
            throw new BadRequestException("Fnr mangler eller er ugyldig");
        }

        pepClient.sjekkLeseTilgangTilFnr(fnr);

        return aktorService.getAktorId(fnr).get();
    }

}
