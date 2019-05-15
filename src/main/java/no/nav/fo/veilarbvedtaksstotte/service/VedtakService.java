package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.apiapp.security.veilarbabac.Bruker;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarbvedtaksstotte.client.*;
import no.nav.fo.veilarbvedtaksstotte.domain.*;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.OpplysningsType;
import no.nav.fo.veilarbvedtaksstotte.repository.OpplysningerRepository;
import no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.sbl.jdbc.Transactor;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static no.nav.fo.veilarbvedtaksstotte.domain.enums.OpplysningsType.*;
import static no.nav.fo.veilarbvedtaksstotte.utils.ValideringUtils.validerFnr;

@Service
public class VedtakService {

    private VedtaksstotteRepository vedtaksstotteRepository;
    private OpplysningerRepository opplysningerRepository;
    private AuthService authService;
    private AktorService aktorService;
    private DokumentClient dokumentClient;
    private SAFClient safClient;
    private PersonClient personClient;
    private CVClient cvClient;
    private RegistreringClient registreringClient;
    private EgenvurderingClient egenvurderingClient;
    private ArenaClient arenaClient;
    private VeilederService veilederService;
    private MalTypeService malTypeService;
    private KafkaService kafkaService;
    private Transactor transactor;

    @Inject
    public VedtakService(VedtaksstotteRepository vedtaksstotteRepository,
                         AktorService aktorService,
                         OpplysningerRepository opplysningerRepository,
                         AuthService authService,
                         DokumentClient dokumentClient,
                         SAFClient safClient, PersonClient personClient,
                         CVClient cvClient,
                         RegistreringClient registreringClient,
                         EgenvurderingClient egenvurderingClient,
                         ArenaClient arenaClient,
                         VeilederService veilederService,
                         MalTypeService malTypeService,
                         KafkaService kafkaService,
                         Transactor transactor) {
        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.aktorService = aktorService;
        this.authService = authService;
        this.opplysningerRepository = opplysningerRepository;
        this.dokumentClient = dokumentClient;
        this.safClient = safClient;
        this.personClient = personClient;
        this.arenaClient = arenaClient;
        this.cvClient = cvClient;
        this.registreringClient = registreringClient;
        this.egenvurderingClient = egenvurderingClient;
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

        lagreOyeblikksbildeForOpplysninger(fnr, vedtak);

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

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(aktorId); //TODO: hente opplysninger

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

        vedtaksstotteRepository.oppdaterUtkast(utkast.getId(), nyttUtkast); //TODO: oppdater opplysninger
    }

    public void slettUtkast(String fnr) {
        validerFnr(fnr);

        Bruker bruker = authService.sjekkSkrivetilgangTilBruker(fnr);

        // TODO KAN VEM SOM HELST SOM HAR TILGANG TIL BRUKEREN SLETTE UTKAST?

        if (!vedtaksstotteRepository.slettUtkast(bruker.getAktoerId())) {
            throw new NotFoundException(format("Fante ikke utkast for bruker med aktorId: %s", bruker.getAktoerId()));
        }
        //TODO: slette opplysninger
    }

    public List<Vedtak> hentVedtak(String fnr) {
        validerFnr(fnr);

        Bruker bruker = authService.sjekkSkrivetilgangTilBruker(fnr);

        return vedtaksstotteRepository.hentVedtak(bruker.getAktoerId()); //TODO: hente opplysninger
    }

    public byte[] produserDokumentUtkast(String fnr) {
        validerFnr(fnr);

        Bruker bruker = authService.sjekkSkrivetilgangTilBruker(fnr);

        SendDokumentDTO sendDokumentDTO =
                Optional.ofNullable(vedtaksstotteRepository.hentUtkast(bruker.getAktoerId()))
                        .map(vedtak -> lagDokumentDTO(vedtak, dokumentPerson(fnr)))
                        .orElseThrow(() -> new NotFoundException("Fant ikke vedtak å forhandsvise for bruker"));

        return dokumentClient.produserDokumentUtkast(sendDokumentDTO); //TODO: her må det vel gjøres noe med opplysninger?
    }

    public List<Opplysning> hentOpplysningerForVedtak(String fnr, long vedtakId) {
        authService.sjekkSkrivetilgangTilBruker(fnr);
        return opplysningerRepository.hentOpplysningerForVedtak(vedtakId);
    }

    private DokumentPerson dokumentPerson(String fnr) {
        PersonNavn navn = personClient.hentNavn(fnr);

        return new DokumentPerson()
                .setFnr(fnr)
                .setNavn(navn.getSammensattNavn());
    }

    private SendDokumentDTO lagDokumentDTO(Vedtak vedtak, DokumentPerson dokumentPerson) {
        List<String> oyeblikksbilde = hentOyeblikksbilde(vedtak.getId());

        return new SendDokumentDTO()
                .setBegrunnelse(vedtak.getBegrunnelse())
                .setVeilederEnhet(vedtak.getVeilederEnhetId())
                .setOpplysninger(oyeblikksbilde)
                .setMalType(malTypeService.utledMalTypeFraVedtak(vedtak))
                .setBruker(dokumentPerson)
                .setMottaker(dokumentPerson);
    }

    private boolean lagreOyeblikksbildeForOpplysninger(String fnr, Vedtak vedtak) {
        final String registreringData = registreringClient.hentRegistrering(fnr);
        final String cvData = cvClient.hentCV(fnr);
        final String egenvurderingData = egenvurderingClient.hentEgenvurdering(fnr);

        List<OpplysningsType> opplysningsTyper = vedtak.getOpplysningsTyper();

        if (opplysningsTyper.contains(REGISTRERINGSINFO) && registreringData.isEmpty()) {
            return false;
        }

        if (opplysningsTyper.contains(CV) && cvData.isEmpty()) {
            return false;
        }

        if (opplysningsTyper.contains(JOBBPROFIL) && cvData.isEmpty()) {
            return false;
        }

        if (opplysningsTyper.contains(EGENVURDERING) && egenvurderingData.isEmpty()) {
            return false;
        }

        lagreOpplysning(vedtak.getId(), REGISTRERINGSINFO, registreringData);
        lagreOpplysning(vedtak.getId(), CV, cvData);
        lagreOpplysning(vedtak.getId(), JOBBPROFIL, cvData);
        lagreOpplysning(vedtak.getId(), EGENVURDERING, egenvurderingData);
        return true;
    }

    private void lagreOpplysning(long vedtakId, OpplysningsType opplysningsType, String opplysningData) {
        final Opplysning opplysning = new Opplysning()
                .setVedtakId(vedtakId)
                .setOpplysningsType(opplysningsType)
                .setJson(opplysningData);

        opplysningerRepository.lagOpplysning(opplysning);
    }

    private List<String> hentOyeblikksbilde(long vedtakId) {
        List<Opplysning> opplysninger = opplysningerRepository.hentOpplysningerForVedtak(vedtakId);
        List<AndreOpplysninger> andreOpplysninger = opplysningerRepository.hentAndreOpplysningerForVedtak(vedtakId);
        List<String> oyeblikksbilde = new ArrayList<>();

        opplysninger
                .forEach(opplysning -> oyeblikksbilde.add(opplysning.getJson()));
        andreOpplysninger
                .forEach(opplysning -> oyeblikksbilde.add(opplysning.getTekst()));

        return oyeblikksbilde;
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
