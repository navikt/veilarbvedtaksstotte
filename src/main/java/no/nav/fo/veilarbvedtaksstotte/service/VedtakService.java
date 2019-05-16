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
import java.util.*;
import java.util.stream.Collectors;

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

        Vedtak oppdaterVedtak = hentOgOppdaterOpplysninger(fnr, vedtak);

        String oppfolgingsenhetId = arenaClient.oppfolgingsenhet(fnr);

        if (!oppdaterVedtak.getVeilederEnhetId().equals(oppfolgingsenhetId)) {
            oppdaterVedtak.setVeilederEnhetId(oppfolgingsenhetId);
        }

        if (!veilederService.hentVeilederIdentFraToken().equals(oppdaterVedtak.getVeilederIdent())) {
            oppdaterVedtak.setVeilederIdent(veilederService.hentVeilederIdentFraToken());
        }

        // TODO oppdater til ny status for "sender" + optimistic lock? Unngå potensielt å sende flere ganger

        vedtaksstotteRepository.oppdaterUtkast(oppdaterVedtak.getId(), oppdaterVedtak);

        SendDokumentDTO sendDokumentDTO = lagDokumentDTO(oppdaterVedtak, dokumentPerson(fnr));
        DokumentSendtDTO dokumentSendt = dokumentClient.sendDokument(sendDokumentDTO);

        transactor.inTransaction(() -> {
            vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(aktorId);
            vedtaksstotteRepository.markerVedtakSomSendt(oppdaterVedtak.getId(), dokumentSendt);
        });

        kafkaService.sendVedtak(oppdaterVedtak.getId());

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
        Vedtak nyttUtkast = vedtakDTO.tilVedtakFraUtkast()
                .setVeilederIdent(veilederIdent)
                .setVeilederEnhetId(oppfolgingsenhetId);

        if (utkast == null) {
            throw new NotFoundException(format("Fante ikke utkast å oppdatere for bruker med aktorId: %s", bruker.getAktoerId()));
        }

        transactor.inTransaction(() -> {
            vedtaksstotteRepository.oppdaterUtkast(utkast.getId(), nyttUtkast);
            opplysningerRepository.slettOpplysninger(utkast.getId());
            opplysningerRepository.slettAndreOpplysninger(utkast.getId());
            opplysningerRepository.lagOpplysning(utkast.getOpplysninger());
            opplysningerRepository.lagAnnenOpplysning(utkast.getAnnenOpplysning());
        }
        );
    }

    public void slettUtkast(String fnr) {
        validerFnr(fnr);

        Bruker bruker = authService.sjekkSkrivetilgangTilBruker(fnr);

        Vedtak vedtak = vedtaksstotteRepository.hentUtkast(bruker.getAktoerId());
        transactor.inTransaction(() -> {
            vedtaksstotteRepository.slettUtkast(bruker.getAktoerId());
            opplysningerRepository.slettOpplysninger(vedtak.getId());
            opplysningerRepository.slettAndreOpplysninger(vedtak.getId());
        });
    }

    public List<Vedtak> hentVedtak(String fnr) { //TODO: Lag en egen VedtakListeDTO
        validerFnr(fnr);

        Bruker bruker = authService.sjekkSkrivetilgangTilBruker(fnr);

        return vedtaksstotteRepository.hentVedtak(bruker.getAktoerId()); //TODO: lag public Vedtak hentVedtak(long vedtakId)
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

    public byte[] hentVedtakPdf(String fnr, String dokumentInfoId, String journalpostId) {
        authService.sjekkSkrivetilgangTilBruker(fnr);

        return safClient.hentVedtakPdf(journalpostId, dokumentInfoId);
    }

    public void kafkaTest(String fnr, Innsatsgruppe innsatsgruppe) {
        Bruker bruker = authService.sjekkSkrivetilgangTilBruker(fnr);
        Vedtak vedtak = vedtaksstotteRepository.hentUtkast(bruker.getAktoerId());
        kafkaService.sendVedtak(vedtak.getId());
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

    private Vedtak hentOgOppdaterOpplysninger(String fnr, Vedtak vedtak) {
        // TODO Vi må få OK repsons på _alle_ kilder helst 204 for ikke eksisterende, 200 for ok slik at vi kan feile når utilgjengelig
        // Det skal være OK at data mangler så lenge de ikke finnes, så lenge kall mot tjenestene ikke feiler
        final String registreringData = registreringClient.hentRegistrering(fnr);
        final String cvData = cvClient.hentCV(fnr);
        final String egenvurderingData = egenvurderingClient.hentEgenvurdering(fnr);

        List<Opplysning> opplysninger = vedtak.getOpplysninger();
        Map<OpplysningsType, String> kilder = new HashMap<>();
        kilder.put(CV, cvData);
        kilder.put(JOBBPROFIL, cvData); // TODO
        kilder.put(REGISTRERINGSINFO, registreringData);
        kilder.put(EGENVURDERING, egenvurderingData);

        final List<Opplysning> oppdaterte = Arrays.stream(OpplysningsType.values()).map(opplysningsType ->
                opplysninger.stream()
                        .filter(opplysning -> opplysning.getOpplysningsType().equals(opplysningsType))
                        .findFirst()
                        .map(opplysning -> opplysning.setJson(kilder.get(opplysning.getOpplysningsType())))
                        .orElseGet(() -> new Opplysning()
                                .setOpplysningsType(opplysningsType)
                                .setValgt(false)
                                .setJson(kilder.get(opplysningsType)))
        ).collect(Collectors.toList());

        return vedtak.setOpplysninger(oppdaterte);
    }

    private List<String> hentOyeblikksbilde(long vedtakId) {
        List<Opplysning> opplysninger = opplysningerRepository.hentOpplysningerForVedtak(vedtakId);
        List<AnnenOpplysning> annenOpplysning = opplysningerRepository.hentAndreOpplysningerForVedtak(vedtakId);
        List<String> oyeblikksbilde = new ArrayList<>();

        opplysninger
                .forEach(opplysning -> oyeblikksbilde.add(opplysning.getJson()));
        annenOpplysning
                .forEach(opplysning -> oyeblikksbilde.add(opplysning.getTekst()));

        return oyeblikksbilde;
    }
}
