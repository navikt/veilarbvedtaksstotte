package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbvedtaksstotte.client.DokumentClient;
import no.nav.fo.veilarbvedtaksstotte.client.ModiaContextClient;
import no.nav.fo.veilarbvedtaksstotte.client.PersonClient;
import no.nav.fo.veilarbvedtaksstotte.client.SAFClient;
import no.nav.fo.veilarbvedtaksstotte.domain.*;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.sbl.jdbc.Transactor;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static no.nav.fo.veilarbvedtaksstotte.utils.AktorIdUtils.getAktorIdOrThrow;
import static no.nav.fo.veilarbvedtaksstotte.utils.ValideringUtils.validerFnr;

@Service
public class VedtakService {

    private VedtaksstotteRepository vedtaksstotteRepository;
    private PepClient pepClient;
    private AktorService aktorService;
    private DokumentClient dokumentClient;
    private SAFClient safClient;
    private PersonClient personClient;
    private ModiaContextClient modiaContextClient;
    private VeilederService veilederService;
    private MalTypeService malTypeService;
    private KafkaService kafkaService;
    private Transactor transactor;

    @Inject
    public VedtakService(VedtaksstotteRepository vedtaksstotteRepository,
                         PepClient pepClient,
                         AktorService aktorService,
                         DokumentClient dokumentClient,
                         SAFClient safClient, PersonClient personClient,
                         ModiaContextClient modiaContextClient,
                         VeilederService veilederService,
                         MalTypeService malTypeService,
                         KafkaService kafkaService,
                         Transactor transactor) {
        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.pepClient = pepClient;
        this.aktorService = aktorService;
        this.dokumentClient = dokumentClient;
        this.safClient = safClient;
        this.modiaContextClient = modiaContextClient;
        this.personClient = personClient;
        this.veilederService = veilederService;
        this.malTypeService = malTypeService;
        this.kafkaService = kafkaService;
        this.transactor = transactor;
    }

    public DokumentSendtDTO sendVedtak(String fnr) {

        validerFnr(fnr);

        pepClient.sjekkLeseTilgangTilFnr(fnr);

        String aktorId = getAktorIdOrThrow(aktorService, fnr);

        Vedtak vedtak = vedtaksstotteRepository.hentUtkast(aktorId);

        if (vedtak == null) {
            throw new NotFoundException("Fant ikke vedtak å sende for bruker");
        }

        SendDokumentDTO sendDokumentDTO = lagDokumentDTO(vedtak, dokumentPerson(fnr));
        DokumentSendtDTO dokumentSendt = dokumentClient.sendDokument(sendDokumentDTO);

        transactor.inTransaction(()-> {
                    vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(aktorId);
                    vedtaksstotteRepository.markerVedtakSomSendt(vedtak.getId(), dokumentSendt);
                });

        kafkaService.sendVedtak(vedtak, aktorId);

        return dokumentSendt;
    }

    public Vedtak hentUtkast(String fnr) {
        validerFnr(fnr);

        pepClient.sjekkLeseTilgangTilFnr(fnr);

        String aktorId = getAktorIdOrThrow(aktorService, fnr);

        return vedtaksstotteRepository.hentUtkast(aktorId);

    }

    public void kafkaTest(String fnr, Innsatsgruppe innsatsgruppe) {
        pepClient.sjekkLeseTilgangTilFnr(fnr);

        String aktorId = getAktorIdOrThrow(aktorService, fnr);

        Vedtak vedtak = new Vedtak().setInnsatsgruppe(innsatsgruppe);
        kafkaService.sendVedtak(vedtak, aktorId);
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

    public void slettUtkast(String fnr) {
        validerFnr(fnr);
        pepClient.sjekkLeseTilgangTilFnr(fnr);
        String aktorId = getAktorIdOrThrow(aktorService, fnr);

        // TODO KAN VEM SOM HELST SOM HAR TILGANG TIL BRUKEREN SLETTE UTKAST?

        if(!vedtaksstotteRepository.slettVedtakUtkast(aktorId)){
            throw new NotFoundException("Fante ikke utkast for bruker med aktorId" + aktorId);
        }

    }

    public List<Vedtak> hentVedtak(String fnr) {
        validerFnr(fnr);

        pepClient.sjekkLeseTilgangTilFnr(fnr);
        String aktorId = getAktorIdOrThrow(aktorService, fnr);
        return vedtaksstotteRepository.hentVedtak(aktorId);
    }

    public byte[] produserDokumentUtkast(String fnr) {
        validerFnr(fnr);

        pepClient.sjekkLeseTilgangTilFnr(fnr);

        String aktorId = getAktorIdOrThrow(aktorService, fnr);

        SendDokumentDTO sendDokumentDTO =
                Optional.ofNullable(vedtaksstotteRepository.hentUtkast(aktorId))
                .map(vedtak -> lagDokumentDTO(vedtak, dokumentPerson(fnr)))
                .orElseThrow(()-> new NotFoundException("Fant ikke vedtak å forhandsvise for bruker"));

        return dokumentClient.produserDokumentUtkast(sendDokumentDTO);
    }

    private DokumentPerson dokumentPerson(String fnr){
        PersonNavn navn = personClient.hentNavn(fnr);

        return new DokumentPerson()
                .setFnr(fnr)
                .setNavn(navn.getSammensattNavn());
    }

    private SendDokumentDTO lagDokumentDTO(Vedtak vedtak, DokumentPerson dokumentPerson) {
        return new SendDokumentDTO()
                .setBegrunnelse(vedtak.getBegrunnelse())
                .setVeilederEnhet(vedtak.getVeileder().getEnhetId())
                .setKilder(Arrays.asList("Kilde 1", "Kilde 2"))
                .setMalType(malTypeService.utledMalTypeFraVedtak(vedtak))
                .setBruker(dokumentPerson)
                .setMottaker(dokumentPerson);
    }

    public byte[] hentVedtakPdf(String fnr, String dokumentInfoId, String journalpostId) {
        pepClient.sjekkLeseTilgangTilFnr(fnr);

        return safClient.hentVedtakPdf(journalpostId,dokumentInfoId);
    }
}
