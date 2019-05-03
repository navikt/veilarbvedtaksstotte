package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbvedtaksstotte.client.DokumentClient;
import no.nav.fo.veilarbvedtaksstotte.client.ArenaClient;
import no.nav.fo.veilarbvedtaksstotte.client.PersonClient;
import no.nav.fo.veilarbvedtaksstotte.client.SAFClient;
import no.nav.fo.veilarbvedtaksstotte.domain.*;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.sbl.jdbc.Transactor;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
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
    private ArenaClient arenaClient;
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
                         ArenaClient arenaClient,
                         VeilederService veilederService,
                         MalTypeService malTypeService,
                         KafkaService kafkaService,
                         Transactor transactor) {
        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.pepClient = pepClient;
        this.aktorService = aktorService;
        this.dokumentClient = dokumentClient;
        this.safClient = safClient;
        this.arenaClient = arenaClient;
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

        transactor.inTransaction(() -> {
            vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(aktorId);
            vedtaksstotteRepository.markerVedtakSomSendt(vedtak.getId(), dokumentSendt);
        });

        kafkaService.sendVedtak(vedtak, aktorId);

        return dokumentSendt;
    }

    public void kafkaTest(String fnr, Innsatsgruppe innsatsgruppe) {
        pepClient.sjekkLeseTilgangTilFnr(fnr);

        String aktorId = getAktorIdOrThrow(aktorService, fnr);

        Vedtak vedtak = new Vedtak().setInnsatsgruppe(innsatsgruppe);
        kafkaService.sendVedtak(vedtak, aktorId);
    }

    public void lagUtkast(String fnr) {
        validerFnr(fnr);

        pepClient.sjekkLeseTilgangTilFnr(fnr);

        String aktorId = getAktorIdOrThrow(aktorService, fnr);
        String veilederIdent = veilederService.hentVeilederIdentFraToken();
        String oppfolgingsenhetId = arenaClient.oppfolgingsenhet(fnr);

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(aktorId);

        if(utkast != null){
            throw new RuntimeException(format("Kan ikke lage nytt utkast, brukeren(%s) har allerede et aktivt utkast", aktorId));
        }

        vedtaksstotteRepository.insertUtkast(aktorId, veilederIdent, oppfolgingsenhetId);
    }

    public void oppdaterUtkast(String fnr, VedtakDTO vedtakDTO) {
        validerFnr(fnr);

        pepClient.sjekkLeseTilgangTilFnr(fnr);

        String aktorId = getAktorIdOrThrow(aktorService, fnr);
        String veilederIdent = veilederService.hentVeilederIdentFraToken();
        String oppfolgingsenhetId = arenaClient.oppfolgingsenhet(fnr);

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(aktorId);
        Vedtak nyttUtkast = vedtakDTO.tilVedtak()
                .setVeilederIdent(veilederIdent)
                .setVeilederEnhetId(oppfolgingsenhetId);

        if (utkast == null) {
            throw new NotFoundException(format("Fante ikke utkast å oppdatere for bruker med aktorId: %s", aktorId));
        }

        vedtaksstotteRepository.oppdaterUtkast(utkast.getId(), nyttUtkast);
    }

    public void slettUtkast(String fnr) {
        validerFnr(fnr);
        pepClient.sjekkLeseTilgangTilFnr(fnr);
        String aktorId = getAktorIdOrThrow(aktorService, fnr);

        // TODO KAN VEM SOM HELST SOM HAR TILGANG TIL BRUKEREN SLETTE UTKAST?

        if(!vedtaksstotteRepository.slettUtkast(aktorId)) {
            throw new NotFoundException(format("Fante ikke utkast for bruker med aktorId: %s", aktorId));
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
                        .orElseThrow(() -> new NotFoundException("Fant ikke vedtak å forhandsvise for bruker"));

        return dokumentClient.produserDokumentUtkast(sendDokumentDTO);
    }

    private DokumentPerson dokumentPerson(String fnr) {
        PersonNavn navn = personClient.hentNavn(fnr);

        return new DokumentPerson()
                .setFnr(fnr)
                .setNavn(navn.getSammensattNavn());
    }

    private SendDokumentDTO lagDokumentDTO(Vedtak vedtak, DokumentPerson dokumentPerson) {
        return new SendDokumentDTO()
                .setBegrunnelse(vedtak.getBegrunnelse())
                .setVeilederEnhet(vedtak.getVeilederEnhetId())
                .setKilder(Arrays.asList("Kilde 1", "Kilde 2"))
                .setMalType(malTypeService.utledMalTypeFraVedtak(vedtak))
                .setBruker(dokumentPerson)
                .setMottaker(dokumentPerson);
    }

    public byte[] hentVedtakPdf(String fnr, String dokumentInfoId, String journalpostId) {
        pepClient.sjekkLeseTilgangTilFnr(fnr);
        return safClient.hentVedtakPdf(journalpostId, dokumentInfoId);
    }
}
