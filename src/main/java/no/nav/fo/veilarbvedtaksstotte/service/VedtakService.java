package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.apiapp.security.veilarbabac.Bruker;
import no.nav.fo.veilarbvedtaksstotte.client.*;
import no.nav.fo.veilarbvedtaksstotte.domain.*;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.fo.veilarbvedtaksstotte.repository.OpplysningerRepository;
import no.nav.fo.veilarbvedtaksstotte.repository.OyblikksbildeRepository;
import no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.sbl.jdbc.Transactor;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static no.nav.fo.veilarbvedtaksstotte.domain.enums.KildeType.*;
import static no.nav.fo.veilarbvedtaksstotte.utils.ValideringUtils.validerFnr;

@Service
public class VedtakService {

    private VedtaksstotteRepository vedtaksstotteRepository;
    private OyblikksbildeRepository oyblikksbildeRepository;
    private OpplysningerRepository opplysningerRepository;
    private AuthService authService;
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
                         OpplysningerRepository opplysningerRepository,
                         OyblikksbildeRepository oyblikksbildeRepository,
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
        this.opplysningerRepository = opplysningerRepository;
        this.oyblikksbildeRepository = oyblikksbildeRepository;
        this.authService = authService;
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

        long vedtakId = vedtak.getId();

        lagreOyblikksbilde(fnr, vedtakId);

        String oppfolgingsenhetId = arenaClient.oppfolgingsenhet(fnr);
        vedtak.setVeilederEnhetId(oppfolgingsenhetId);
        vedtak.setVeilederIdent(veilederService.hentVeilederIdentFraToken());

        // TODO oppdater til ny status for "sender" + optimistic lock? Unngå potensielt å sende flere ganger

        vedtaksstotteRepository.oppdaterUtkast(vedtakId, vedtak);

        SendDokumentDTO sendDokumentDTO = lagDokumentDTO(vedtak, dokumentPerson(fnr));
        DokumentSendtDTO dokumentSendt = dokumentClient.sendDokument(sendDokumentDTO);

        transactor.inTransaction(() -> {
            vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(aktorId);
            vedtaksstotteRepository.markerVedtakSomSendt(vedtakId, dokumentSendt);
        });

        kafkaService.sendVedtak(vedtakId);

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

        if (nyttUtkast.getInnsatsgruppe() == Innsatsgruppe.VARIG_TILPASSET_INNSATS) {
            nyttUtkast.setHovedmal(null);
        }

        if (utkast == null) {
            throw new NotFoundException(format("Fante ikke utkast å oppdatere for bruker med aktorId: %s", bruker.getAktoerId()));
        }

        transactor.inTransaction(() -> {
            vedtaksstotteRepository.oppdaterUtkast(utkast.getId(), nyttUtkast);
            opplysningerRepository.slettOpplysninger(utkast.getId());
            opplysningerRepository.lagOpplysninger(vedtakDTO.getOpplysninger(), utkast.getId());
        });
    }

    public void slettUtkast(String fnr) {
        validerFnr(fnr);

        Bruker bruker = authService.sjekkSkrivetilgangTilBruker(fnr);

        Vedtak vedtak = vedtaksstotteRepository.hentUtkast(bruker.getAktoerId());
        transactor.inTransaction(() -> {
            vedtaksstotteRepository.slettUtkast(bruker.getAktoerId());
            opplysningerRepository.slettOpplysninger(vedtak.getId());
        });
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

    public List<Oyblikksbilde> hentOyblikksbildeForVedtak(String fnr, long vedtakId) {
        authService.sjekkSkrivetilgangTilBruker(fnr);
        return oyblikksbildeRepository.hentOyblikksbildeForVedtak(vedtakId);
    }

    public byte[] hentVedtakPdf(String fnr, String dokumentInfoId, String journalpostId) {
        authService.sjekkSkrivetilgangTilBruker(fnr);
        return safClient.hentVedtakPdf(journalpostId, dokumentInfoId);
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
                .setOpplysninger(vedtak.getOpplysninger())
                .setMalType(malTypeService.utledMalTypeFraVedtak(vedtak))
                .setBruker(dokumentPerson)
                .setMottaker(dokumentPerson);
    }

    private void lagreOyblikksbilde(String fnr, long vedtakId) {
        final String cvData = cvClient.hentCV(fnr);
        final String registreringData = registreringClient.hentRegistrering(fnr);
        final String egenvurderingData = egenvurderingClient.hentEgenvurdering(fnr);

        List<Oyblikksbilde> oyblikksbilde = Arrays.asList(
                new Oyblikksbilde(vedtakId, CV_OG_JOBBPROFIL, cvData),
                new Oyblikksbilde(vedtakId, REGISTRERINGSINFO, registreringData),
                new Oyblikksbilde(vedtakId, EGENVURDERING, egenvurderingData)
        );

        oyblikksbildeRepository.lagOyblikksbilde(oyblikksbilde);
    }

}
