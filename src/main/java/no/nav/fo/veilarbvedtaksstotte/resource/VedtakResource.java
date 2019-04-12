package no.nav.fo.veilarbvedtaksstotte.resource;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbvedtaksstotte.client.DokumentClient;
import no.nav.fo.veilarbvedtaksstotte.domain.DokumentPerson;
import no.nav.fo.veilarbvedtaksstotte.domain.SendDokumentDTO;
import no.nav.fo.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.fo.veilarbvedtaksstotte.domain.VedtakDTO;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.MalType;
import no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.fo.veilarbvedtaksstotte.service.VeilederService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.*;
import java.util.Arrays;

import static no.nav.fo.veilarbvedtaksstotte.utils.AktorIdUtils.getAktorIdOrThrow;
import static no.nav.fo.veilarbvedtaksstotte.utils.ValideringUtils.validerFnr;

@Component
@Slf4j
@Path("/vedtak")
public class VedtakResource {

    private VedtaksstotteRepository vedtaksstotteRepository;
    private PepClient pepClient;
    private AktorService aktorService;
    private VeilederService veilederService;
    private DokumentClient dokumentClient;

    @Inject
    public VedtakResource(VedtaksstotteRepository vedtaksstotteRepository,
                          PepClient pepClient,
                          AktorService aktorService,
                          VeilederService veilederService,
                          DokumentClient dokumentClient) {
        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.pepClient = pepClient;
        this.aktorService = aktorService;
        this.veilederService = veilederService;
        this.dokumentClient = dokumentClient;
    }

    @POST
    @Path("/send/{fnr}")
    public void sendVedtak(@PathParam("fnr") String fnr) {

        validerFnr(fnr);

        pepClient.sjekkLeseTilgangTilFnr(fnr);

        String aktorId = getAktorIdOrThrow(aktorService, fnr);

        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(aktorId, true);

        if (vedtak == null) {
            throw new NotFoundException("Fant ikke vedtak å sende for bruker");
        }

        DokumentPerson dokumentPerson = new DokumentPerson();
        dokumentPerson.setFnr(fnr);
        dokumentPerson.setNavn("TEST PERSON");

        SendDokumentDTO sendDokumentDTO = new SendDokumentDTO()
                .setBegrunnelse(vedtak.getBegrunnelse())
                .setVeilederEnhet(vedtak.getVeileder().getEnhetId())
                .setKilder(Arrays.asList("Kilde 1", "Kilde 2"))
                .setMalType(MalType.STANDARD_INNSATS_SKAFFE_ARBEID)
                .setBruker(dokumentPerson)
                .setMottaker(dokumentPerson);

        dokumentClient.sendDokument(sendDokumentDTO);

        vedtaksstotteRepository.markerVedtakSomSendt(vedtak.getId());

    }

    @GET
    @Path("/{fnr}")
    public Vedtak hentVedtak(@PathParam("fnr") String fnr) {

        validerFnr(fnr);

        pepClient.sjekkLeseTilgangTilFnr(fnr);

        String aktorId = getAktorIdOrThrow(aktorService, fnr);

        return vedtaksstotteRepository.hentVedtak(aktorId, false);
    }

    @PUT
    @Path("/{fnr}")
    public void upsertVedtak(@PathParam("fnr") String fnr, VedtakDTO vedtakDTO) {

        validerFnr(fnr);

        pepClient.sjekkLeseTilgangTilFnr(fnr);

        String aktorId = getAktorIdOrThrow(aktorService, fnr);

        Vedtak vedtak = vedtakDTO.tilVedtak()
                .setVeileder(veilederService.hentVeilederFraToken());

        vedtaksstotteRepository.upsertUtkast(aktorId, vedtak);

    }

}
