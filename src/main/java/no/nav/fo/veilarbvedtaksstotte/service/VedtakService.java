package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbvedtaksstotte.client.DokumentClient;
import no.nav.fo.veilarbvedtaksstotte.client.ModiaContextClient;
import no.nav.fo.veilarbvedtaksstotte.client.PersonClient;
import no.nav.fo.veilarbvedtaksstotte.domain.*;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.util.Arrays;

import static no.nav.fo.veilarbvedtaksstotte.utils.AktorIdUtils.getAktorIdOrThrow;
import static no.nav.fo.veilarbvedtaksstotte.utils.ValideringUtils.validerFnr;

@Service
public class VedtakService {

    private VedtaksstotteRepository vedtaksstotteRepository;
    private PepClient pepClient;
    private AktorService aktorService;
    private DokumentClient dokumentClient;
    private PersonClient personClient;
    private ModiaContextClient modiaContextClient;
    private VeilederService veilederService;
    private MalTypeService malTypeService;
    private KafkaService kafkaService;

    @Inject
    public VedtakService(VedtaksstotteRepository vedtaksstotteRepository,
                         PepClient pepClient,
                         AktorService aktorService,
                         DokumentClient dokumentClient,
                         PersonClient personClient,
                         ModiaContextClient modiaContextClient,
                         VeilederService veilederService,
                         MalTypeService malTypeService, KafkaService kafkaService) {
        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.pepClient = pepClient;
        this.aktorService = aktorService;
        this.dokumentClient = dokumentClient;
        this.modiaContextClient = modiaContextClient;
        this.personClient = personClient;
        this.veilederService = veilederService;
        this.malTypeService = malTypeService;
        this.kafkaService = kafkaService;
    }

    public DokumentSendtDTO sendVedtak(String fnr) {

        validerFnr(fnr);

        pepClient.sjekkLeseTilgangTilFnr(fnr);

        String aktorId = getAktorIdOrThrow(aktorService, fnr);

        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(aktorId, true);

        if (vedtak == null) {
            throw new NotFoundException("Fant ikke vedtak å sende for bruker");
        }

        PersonNavn navn = personClient.hentNavn(fnr);

        DokumentPerson dokumentPerson = new DokumentPerson();
        dokumentPerson.setFnr(fnr);
        dokumentPerson.setNavn(navn.getSammensattNavn());

        SendDokumentDTO sendDokumentDTO = new SendDokumentDTO()
                .setBegrunnelse(vedtak.getBegrunnelse())
                .setVeilederEnhet(vedtak.getVeileder().getEnhetId())
                .setKilder(Arrays.asList("Kilde 1", "Kilde 2"))
                .setMalType(malTypeService.utledMalTypeFraVedtak(vedtak))
                .setBruker(dokumentPerson)
                .setMottaker(dokumentPerson);

        DokumentSendtDTO dokumentSendt = dokumentClient.sendDokument(sendDokumentDTO);

        vedtaksstotteRepository.markerVedtakSomSendt(vedtak.getId(), dokumentSendt);

        kafkaService.sendVedtak(vedtak, aktorId);

        return dokumentSendt;
    }

    public void kafkaTest(String fnr, Innsatsgruppe innsatsgruppe) {
        pepClient.sjekkLeseTilgangTilFnr(fnr);

        String aktorId = getAktorIdOrThrow(aktorService, fnr);

        Vedtak vedtak = new Vedtak().setInnsatsgruppe(innsatsgruppe);
        kafkaService.sendVedtak(vedtak, aktorId);
    }

    public Vedtak hentVedtak(String fnr) {
        validerFnr(fnr);

        pepClient.sjekkLeseTilgangTilFnr(fnr);

        String aktorId = getAktorIdOrThrow(aktorService, fnr);

        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(aktorId, false);

        if (vedtak == null) {
            throw new NotFoundException("Fant ikke vedtak til bruker");
        }

        return vedtak;
    }

    public void upsertVedtak(String fnr, VedtakDTO vedtakDTO) {
        validerFnr(fnr);

        pepClient.sjekkLeseTilgangTilFnr(fnr);

        String aktorId = getAktorIdOrThrow(aktorService, fnr);
        Veileder veileder = veilederService.hentVeilederFraToken();
        veileder.setEnhetId(modiaContextClient.aktivEnhet());

        Vedtak vedtak = vedtakDTO.tilVedtak()
                .setVeileder(veileder);

        vedtaksstotteRepository.upsertUtkast(aktorId, vedtak);
    }

}
