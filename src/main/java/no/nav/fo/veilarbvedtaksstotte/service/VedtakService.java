package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.apiapp.security.veilarbabac.Bruker;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbvedtaksstotte.client.ArenaClient;
import no.nav.fo.veilarbvedtaksstotte.client.DokumentClient;
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
import static no.nav.fo.veilarbvedtaksstotte.utils.ValideringUtils.validerFnr;

@Service
public class VedtakService {

    private VedtaksstotteRepository vedtaksstotteRepository;
    private AuthService authService;
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
                         AuthService authService,
                         AktorService aktorService,
                         DokumentClient dokumentClient,
                         SAFClient safClient, PersonClient personClient,
                         ArenaClient arenaClient,
                         VeilederService veilederService,
                         MalTypeService malTypeService,
                         KafkaService kafkaService,
                         Transactor transactor) {
        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.authService = authService;
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

        Bruker bruker = authService.sjekkSkrivetilgangTilBruker(fnr);
        String aktorId = bruker.getAktoerId();

        Vedtak vedtak = vedtaksstotteRepository.hentUtkast(aktorId);

        if (vedtak == null) {
            throw new NotFoundException("Fant ikke vedtak å sende for bruker");
        }

        String oppfolgingsenhetId = arenaClient.oppfolgingsenhet(fnr);

        if (!vedtak.getVeilederEnhetId().equals(oppfolgingsenhetId)) {
            vedtak.setVeilederEnhetId(oppfolgingsenhetId);
        }

        if (!veilederService.hentVeilederIdentFraToken().equals(vedtak.getVeilederIdent())) {
            vedtak.setVeilederIdent(veilederService.hentVeilederIdentFraToken());
        }

        // TODO oppdater til ny status for "sender" + optimistic lock? Unngå potensielt å sende flere ganger

        vedtaksstotteRepository.oppdaterUtkast(vedtak.getId(), vedtak);

        SendDokumentDTO sendDokumentDTO = lagDokumentDTO(vedtak, dokumentPerson(fnr));
        DokumentSendtDTO dokumentSendt = dokumentClient.sendDokument(sendDokumentDTO);

        transactor.inTransaction(() -> {
            vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(aktorId);
            vedtaksstotteRepository.markerVedtakSomSendt(vedtak.getId(), dokumentSendt);
        });

        kafkaService.sendVedtak(vedtak.getId());

        return dokumentSendt;
    }

    public void lagUtkast(String fnr) {
        validerFnr(fnr);

        Bruker bruker = authService.sjekkSkrivetilgangTilBruker(fnr);
        String aktorId = bruker.getAktoerId();
        String veilederIdent = veilederService.hentVeilederIdentFraToken();
        String oppfolgingsenhetId = arenaClient.oppfolgingsenhet(fnr);

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(aktorId);

        if (utkast != null) {
            throw new RuntimeException(format("Kan ikke lage nytt utkast, brukeren(%s) har allerede et aktivt utkast", aktorId));
        }

        vedtaksstotteRepository.insertUtkast(aktorId, veilederIdent, oppfolgingsenhetId);
    }

    public void oppdaterUtkast(String fnr, VedtakDTO vedtakDTO) {
        validerFnr(fnr);

        Bruker bruker = authService.sjekkSkrivetilgangTilBruker(fnr);

        String veilederIdent = veilederService.hentVeilederIdentFraToken();

        String oppfolgingsenhetId = arenaClient.oppfolgingsenhet(fnr);

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(bruker.getAktoerId());
        Vedtak nyttUtkast = vedtakDTO.tilVedtak()
                .setVeilederIdent(veilederIdent)
                .setVeilederEnhetId(oppfolgingsenhetId);

        if (utkast == null) {
            throw new NotFoundException(format("Fante ikke utkast å oppdatere for bruker med aktorId: %s", bruker.getAktoerId()));
        }

        vedtaksstotteRepository.oppdaterUtkast(utkast.getId(), nyttUtkast);
    }

    public void slettUtkast(String fnr) {
        validerFnr(fnr);

        Bruker bruker = authService.sjekkSkrivetilgangTilBruker(fnr);

        // TODO KAN VEM SOM HELST SOM HAR TILGANG TIL BRUKEREN SLETTE UTKAST?

        if (!vedtaksstotteRepository.slettUtkast(bruker.getAktoerId())) {
            throw new NotFoundException(format("Fante ikke utkast for bruker med aktorId: %s", bruker.getAktoerId()));
        }
    }

    public List<Vedtak> hentVedtak(String fnr) {
        validerFnr(fnr);

        Bruker bruker = authService.sjekkSkrivetilgangTilBruker(fnr);

        return vedtaksstotteRepository.hentVedtak(bruker.getAktoerId());
    }

    public byte[] produserDokumentUtkast(String fnr) {
        validerFnr(fnr);

        Bruker bruker = authService.sjekkSkrivetilgangTilBruker(fnr);

        SendDokumentDTO sendDokumentDTO =
                Optional.ofNullable(vedtaksstotteRepository.hentUtkast(bruker.getAktoerId()))
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
        authService.sjekkSkrivetilgangTilBruker(fnr);

        return safClient.hentVedtakPdf(journalpostId, dokumentInfoId);
    }

    public void kafkaTest(String fnr, Innsatsgruppe innsatsgruppe) {
        Bruker bruker = authService.sjekkSkrivetilgangTilBruker(fnr);
        Vedtak vedtak = vedtaksstotteRepository.hentUtkast(bruker.getAktoerId());
        kafkaService.sendVedtak(vedtak.getId());
    }
}
