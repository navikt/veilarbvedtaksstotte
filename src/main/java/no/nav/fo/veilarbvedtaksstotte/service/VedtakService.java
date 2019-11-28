package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.apiapp.security.veilarbabac.Bruker;
import no.nav.fo.veilarbvedtaksstotte.client.*;
import no.nav.fo.veilarbvedtaksstotte.domain.*;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.fo.veilarbvedtaksstotte.repository.KilderRepository;
import no.nav.fo.veilarbvedtaksstotte.repository.OyblikksbildeRepository;
import no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.sbl.jdbc.Transactor;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static no.nav.fo.veilarbvedtaksstotte.domain.enums.KildeType.*;
import static no.nav.fo.veilarbvedtaksstotte.utils.JsonUtils.toJson;
import static no.nav.fo.veilarbvedtaksstotte.utils.ValideringUtils.validerFnr;

@Service
public class VedtakService {

    private VedtaksstotteRepository vedtaksstotteRepository;
    private OyblikksbildeRepository oyblikksbildeRepository;
    private KilderRepository kilderRepository;
    private AuthService authService;
    private DokumentClient dokumentClient;
    private SAFClient safClient;
    private PersonClient personClient;
    private VeiledereOgEnhetClient veiledereOgEnhetClient;
    private CVClient cvClient;
    private RegistreringClient registreringClient;
    private EgenvurderingClient egenvurderingClient;
    private VeilederService veilederService;
    private MalTypeService malTypeService;
    private KafkaService kafkaService;
    private MetricsService metricsService;
    private Transactor transactor;

    @Inject
    public VedtakService(VedtaksstotteRepository vedtaksstotteRepository,
                         KilderRepository kilderRepository,
                         OyblikksbildeRepository oyblikksbildeRepository,
                         AuthService authService,
                         DokumentClient dokumentClient,
                         SAFClient safClient, PersonClient personClient,
                         VeiledereOgEnhetClient veiledereOgEnhetClient, CVClient cvClient,
                         RegistreringClient registreringClient,
                         EgenvurderingClient egenvurderingClient,
                         VeilederService veilederService,
                         MalTypeService malTypeService,
                         KafkaService kafkaService,
                         MetricsService metricsService, Transactor transactor) {
        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.kilderRepository = kilderRepository;
        this.oyblikksbildeRepository = oyblikksbildeRepository;
        this.authService = authService;
        this.dokumentClient = dokumentClient;
        this.safClient = safClient;
        this.personClient = personClient;
        this.veiledereOgEnhetClient = veiledereOgEnhetClient;
        this.cvClient = cvClient;
        this.registreringClient = registreringClient;
        this.egenvurderingClient = egenvurderingClient;
        this.veilederService = veilederService;
        this.malTypeService = malTypeService;
        this.kafkaService = kafkaService;
        this.metricsService = metricsService;
        this.transactor = transactor;
    }

    public DokumentSendtDTO sendVedtak(String fnr, String beslutter) {

        validerFnr(fnr);

        AuthKontekst authKontekst = authService.sjekkTilgang(fnr);
        String aktorId = authKontekst.getBruker().getAktoerId();

        Vedtak vedtak = vedtaksstotteRepository.hentUtkast(aktorId);

        if (vedtak == null) {
            throw new NotFoundException("Fant ikke vedtak å sende for bruker");
        }

        if (skalHaBeslutter(vedtak.getInnsatsgruppe()) && (beslutter == null || beslutter.isEmpty())) {
            throw new IllegalArgumentException("Vedtak kan ikke bli sendt uten beslutter");
        }

        long vedtakId = vedtak.getId();

        lagreOyblikksbilde(fnr, vedtakId);

        String oppfolgingsenhetId = authKontekst.getOppfolgingsenhet();
        if (!vedtak.getVeilederEnhetId().equals(oppfolgingsenhetId)) {
            String enhetNavn = veiledereOgEnhetClient.hentEnhetNavn(oppfolgingsenhetId);
            vedtak.setVeilederEnhetNavn(enhetNavn);
        }

        vedtak.setVeilederEnhetId(oppfolgingsenhetId);
        vedtak.setVeilederIdent(veilederService.hentVeilederIdentFraToken());

        // TODO oppdater til ny status for "sender" + optimistic lock? Unngå potensielt å sende flere ganger

        vedtaksstotteRepository.oppdaterUtkast(vedtakId, vedtak);

        SendDokumentDTO sendDokumentDTO = lagDokumentDTO(vedtak, dokumentPerson(fnr));
        DokumentSendtDTO dokumentSendt = dokumentClient.sendDokument(sendDokumentDTO);

        transactor.inTransaction(() -> {
            vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(aktorId);
            vedtaksstotteRepository.ferdigstillVedtak(vedtakId, dokumentSendt, beslutter);
        });

        kafkaService.sendVedtak(vedtakId);

        metricsService.rapporterVedtakSendt(vedtak);
        metricsService.rapporterTidFraRegistrering(vedtak, aktorId, fnr);
        metricsService.rapporterVedtakSendtSykmeldtUtenArbeidsgiver(vedtak, fnr);

        return dokumentSendt;
    }

    public void lagUtkast(String fnr) {
        validerFnr(fnr);

        AuthKontekst authKontekst = authService.sjekkTilgang(fnr);
        String aktorId = authKontekst.getBruker().getAktoerId();
        Vedtak utkast = vedtaksstotteRepository.hentUtkast(aktorId);

        if (utkast != null) {
            throw new RuntimeException(format("Kan ikke lage nytt utkast, brukeren(%s) har allerede et aktivt utkast", aktorId));
        }

        String veilederIdent = veilederService.hentVeilederIdentFraToken();
        String oppfolgingsenhetId = authKontekst.getOppfolgingsenhet();
        String enhetNavn = veiledereOgEnhetClient.hentEnhetNavn(oppfolgingsenhetId);

        vedtaksstotteRepository.opprettUtakst(aktorId, veilederIdent, oppfolgingsenhetId, enhetNavn);
    }

    public void oppdaterUtkast(String fnr, VedtakDTO vedtakDTO) {
        validerFnr(fnr);

        AuthKontekst authKontekst = authService.sjekkTilgang(fnr);
        Bruker bruker = authKontekst.getBruker();
        String veilederIdent = veilederService.hentVeilederIdentFraToken();
        String oppfolgingsenhetId = authKontekst.getOppfolgingsenhet();
        Vedtak utkast = vedtaksstotteRepository.hentUtkast(bruker.getAktoerId());
        Vedtak nyttUtkast = vedtakDTO.tilVedtakFraUtkast()
                .setVeilederIdent(veilederIdent)
                .setVeilederEnhetId(oppfolgingsenhetId);

        if (utkast == null) {
            throw new NotFoundException(format("Fant ikke utkast å oppdatere for bruker med aktorId: %s", bruker.getAktoerId()));
        }

        if (!utkast.getVeilederEnhetId().equals(oppfolgingsenhetId)) {
            String enhetNavn = veiledereOgEnhetClient.hentEnhetNavn(oppfolgingsenhetId);
            nyttUtkast.setVeilederEnhetNavn(enhetNavn);
        }

        if (nyttUtkast.getInnsatsgruppe() == Innsatsgruppe.VARIG_TILPASSET_INNSATS) {
            nyttUtkast.setHovedmal(null);
        }

        transactor.inTransaction(() -> {
            vedtaksstotteRepository.oppdaterUtkast(utkast.getId(), nyttUtkast);
            kilderRepository.slettKilder(utkast.getId());
            kilderRepository.lagKilder(vedtakDTO.getOpplysninger(), utkast.getId());
        });
    }

    public void slettUtkast(String fnr) {
        validerFnr(fnr);

        Bruker bruker = authService.sjekkTilgang(fnr).getBruker();
        Vedtak vedtak = vedtaksstotteRepository.hentUtkast(bruker.getAktoerId());

        transactor.inTransaction(() -> {
            vedtaksstotteRepository.slettUtkast(bruker.getAktoerId());
            kilderRepository.slettKilder(vedtak.getId());
        });

        metricsService.rapporterUtkastSlettet();
    }

    public List<Vedtak> hentVedtak(String fnr) {
        validerFnr(fnr);

        Bruker bruker = authService.sjekkTilgang(fnr).getBruker();

        List<Vedtak> vedtak = vedtaksstotteRepository.hentVedtak(bruker.getAktoerId());

        flettInnVeilederNavn(vedtak);

        return vedtak;
    }

    public byte[] produserDokumentUtkast(String fnr) {
        validerFnr(fnr);

        Bruker bruker = authService.sjekkTilgang(fnr).getBruker();

        SendDokumentDTO sendDokumentDTO =
                Optional.ofNullable(vedtaksstotteRepository.hentUtkast(bruker.getAktoerId()))
                        .map(vedtak -> lagDokumentDTO(vedtak, dokumentPerson(fnr)))
                        .orElseThrow(() -> new NotFoundException("Fant ikke vedtak å forhandsvise for bruker"));

        return dokumentClient.produserDokumentUtkast(sendDokumentDTO);
    }

    public List<Oyblikksbilde> hentOyblikksbildeForVedtak(String fnr, long vedtakId) {
        authService.sjekkTilgang(fnr);
        return oyblikksbildeRepository.hentOyblikksbildeForVedtak(vedtakId);
    }

    public byte[] hentVedtakPdf(String fnr, String dokumentInfoId, String journalpostId) {
        authService.sjekkTilgang(fnr);
        return safClient.hentVedtakPdf(journalpostId, dokumentInfoId);
    }

    public boolean harUtkast(String fnr) {
        Bruker bruker = authService.sjekkTilgang(fnr).getBruker();
        return vedtaksstotteRepository.hentUtkast(bruker.getAktoerId()) != null;
    }

    public void behandleAvsluttOppfolging (KafkaAvsluttOppfolging melding ) {
        String aktorId = melding.getAktorId();
        Date sluttDato = melding.getSluttdato();
        vedtaksstotteRepository.slettUtkast(aktorId, sluttDato);
        vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(aktorId, sluttDato);
    }

    private void flettInnVeilederNavn(List<Vedtak> vedtak) {
        vedtak.forEach(v -> {
            Veileder veileder = veiledereOgEnhetClient.hentVeileder(v.getVeilederIdent());
            v.setVeilederNavn(veileder != null ? veileder.getNavn() : null);
        });
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
                .setMalType(malTypeService.utledMalTypeFraVedtak(vedtak, dokumentPerson.getFnr()))
                .setBruker(dokumentPerson)
                .setMottaker(dokumentPerson);
    }

    private boolean skalHaBeslutter(Innsatsgruppe innsatsgruppe) {
        return Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS == innsatsgruppe
                || Innsatsgruppe.VARIG_TILPASSET_INNSATS == innsatsgruppe;
    }

    private void lagreOyblikksbilde(String fnr, long vedtakId) {
        final String cvData = cvClient.hentCV(fnr);
        final String registreringData = toJson(registreringClient.hentRegistreringData(fnr));
        final String egenvurderingData = egenvurderingClient.hentEgenvurdering(fnr);

        List<Oyblikksbilde> oyblikksbilde = Arrays.asList(
                new Oyblikksbilde(vedtakId, CV_OG_JOBBPROFIL, cvData),
                new Oyblikksbilde(vedtakId, REGISTRERINGSINFO, registreringData),
                new Oyblikksbilde(vedtakId, EGENVURDERING, egenvurderingData)
        );

        oyblikksbildeRepository.lagOyblikksbilde(oyblikksbilde);
    }

}
