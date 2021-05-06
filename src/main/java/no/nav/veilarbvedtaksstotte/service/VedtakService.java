package no.nav.veilarbvedtaksstotte.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.OpprettetJournalpostDTO;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient;
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DistribuerJournalpostResponsDTO;
import no.nav.veilarbvedtaksstotte.client.dokument.DokumentSendtDTO;
import no.nav.veilarbvedtaksstotte.client.dokument.ProduserDokumentV2DTO;
import no.nav.veilarbvedtaksstotte.client.dokument.SendDokumentDTO;
import no.nav.veilarbvedtaksstotte.client.dokument.VeilarbdokumentClient;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.Veileder;
import no.nav.veilarbvedtaksstotte.controller.dto.OppdaterUtkastDTO;
import no.nav.veilarbvedtaksstotte.domain.AuthKontekst;
import no.nav.veilarbvedtaksstotte.domain.dialog.SystemMeldingType;
import no.nav.veilarbvedtaksstotte.domain.vedtak.*;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import no.nav.veilarbvedtaksstotte.repository.KilderRepository;
import no.nav.veilarbvedtaksstotte.repository.MeldingRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.veilarbvedtaksstotte.utils.VedtakUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus.GODKJENT_AV_BESLUTTER;
import static no.nav.veilarbvedtaksstotte.utils.InnsatsgruppeUtils.skalHaBeslutter;

@Slf4j
@Service
public class VedtakService {

    private final TransactionTemplate transactor;

    private final VedtaksstotteRepository vedtaksstotteRepository;
    private final BeslutteroversiktRepository beslutteroversiktRepository;
    private final KilderRepository kilderRepository;
    private final MeldingRepository meldingRepository;

    private final VeilarbdokumentClient dokumentClient;
    private final SafClient safClient;

    private final AuthService authService;
    private final UnleashService unleashService;
    private final MetricsService metricsService;

    private final OyeblikksbildeService oyeblikksbildeService;
    private final VeilederService veilederService;
    private final MalTypeService malTypeService;
    private final VedtakStatusEndringService vedtakStatusEndringService;
    private final DokumentServiceV2 dokumentServiceV2;

    @Autowired
    public VedtakService(
            TransactionTemplate transactor,

            VedtaksstotteRepository vedtaksstotteRepository,
            BeslutteroversiktRepository beslutteroversiktRepository,
            KilderRepository kilderRepository,
            MeldingRepository meldingRepository,

            VeilarbdokumentClient dokumentClient,
            SafClient safClient,

            AuthService authService,
            UnleashService unleashService,
            MetricsService metricsService,

            OyeblikksbildeService oyeblikksbildeService,
            VeilederService veilederService,
            MalTypeService malTypeService,
            VedtakStatusEndringService vedtakStatusEndringService,
            DokumentServiceV2 dokumentServiceV2
    ) {
        this.transactor = transactor;

        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.beslutteroversiktRepository = beslutteroversiktRepository;
        this.kilderRepository = kilderRepository;
        this.meldingRepository = meldingRepository;

        this.dokumentClient = dokumentClient;
        this.safClient = safClient;

        this.authService = authService;
        this.unleashService = unleashService;
        this.metricsService = metricsService;

        this.oyeblikksbildeService = oyeblikksbildeService;
        this.veilederService = veilederService;
        this.malTypeService = malTypeService;
        this.vedtakStatusEndringService = vedtakStatusEndringService;
        this.dokumentServiceV2 = dokumentServiceV2;
    }

    @SneakyThrows
    public DokumentSendtDTO fattVedtak(long vedtakId) {
        UtkastetVedtak utkastetVedtak = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);
        AuthKontekst authKontekst = authService.sjekkTilgangTilBrukerOgEnhet(AktorId.of(utkastetVedtak.getAktorId()));
        authService.sjekkErAnsvarligVeilederFor(utkastetVedtak);

        flettInnVedtakInformasjon(utkastetVedtak);

        Vedtak gjeldendeVedtak = vedtaksstotteRepository.hentGjeldendeVedtak(utkastetVedtak.getAktorId());
        validerVedtakForFerdigstillingOgUtsending(utkastetVedtak, gjeldendeVedtak);

        return sendDokumentOgFerdigstill(utkastetVedtak, authKontekst);
    }

    private DokumentSendtDTO sendDokumentOgFerdigstill(UtkastetVedtak utkastetVedtak, AuthKontekst authKontekst) {
        if (brukNyDokIntegrasjon()) {
            log.info(format("Sender og ferdigstiller utkastetVedtak med nye integrasjoner (utkastetVedtak id = %s, aktør id = %s)",
                    utkastetVedtak.getId(), authKontekst.getAktorId()));
            // Oppdaterer utkastetVedtak til "sender" tilstand for å redusere risiko for dupliserte utsendelser av dokument.
            vedtaksstotteRepository.oppdaterSender(utkastetVedtak.getId(), true);
            try {
                return sendDokumentOgFerdigstillV2(utkastetVedtak, authKontekst);
            } finally {
                vedtaksstotteRepository.oppdaterSender(utkastetVedtak.getId(), false);
            }
        } else {
            log.info(format("Sender og ferdigstiller utkastetVedtak med gammel integrasjon (utkastetVedtak id = %s, aktør id = %s)",
                    utkastetVedtak.getId(), authKontekst.getAktorId()));
            return sendDokumentOgFerdigstillV1(utkastetVedtak, authKontekst);
        }
    }

    private boolean brukNyDokIntegrasjon() {
        return EnvironmentUtils.isDevelopment().orElse(false) && unleashService.isNyDokIntegrasjonEnabled();
    }

    private DokumentSendtDTO sendDokumentOgFerdigstillV1(UtkastetVedtak utkastetVedtak, AuthKontekst authKontekst) {
        oyeblikksbildeService.lagreOyeblikksbilde(authKontekst.getFnr(), utkastetVedtak.getId());

        DokumentSendtDTO dokumentSendt = sendDokument(utkastetVedtak, authKontekst.getFnr());

        log.info(String.format("Dokument sendt: journalpostId=%s dokumentId=%s",
                dokumentSendt.getJournalpostId(), dokumentSendt.getDokumentId()));

        transactor.executeWithoutResult((status) -> {
            vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(utkastetVedtak.getAktorId());
            vedtaksstotteRepository.ferdigstillVedtak(utkastetVedtak.getId(), dokumentSendt);
            beslutteroversiktRepository.slettBruker(utkastetVedtak.getId());
        });

        vedtakStatusEndringService.vedtakSendt(utkastetVedtak.getId(), Fnr.of(authKontekst.getFnr()));

        return dokumentSendt;
    }

    private DokumentSendtDTO sendDokument(Vedtak vedtak, String fnr) {
        // Oppdaterer vedtak til "sender" tilstand for å redusere risiko for dupliserte utsendelser av dokument.
        vedtaksstotteRepository.oppdaterSender(vedtak.getId(), true);
        try {
            metricsService.rapporterSendDokument();
            SendDokumentDTO sendDokumentDTO = lagDokumentDTO(vedtak, fnr);
            return dokumentClient.sendDokument(sendDokumentDTO);
        } catch (Exception e) {
            vedtaksstotteRepository.oppdaterSender(vedtak.getId(), false);
            throw e;
        }
    }

    private DokumentSendtDTO sendDokumentOgFerdigstillV2(UtkastetVedtak utkastetVedtak, AuthKontekst authKontekst) {
        long vedtakId = utkastetVedtak.getId();

        oyeblikksbildeService.lagreOyeblikksbilde(authKontekst.getFnr(), vedtakId);

        log.info(format("Journalfører og distribuerer dokument for utkastetVedtak med id=%s for aktør id=%s",
                vedtakId, utkastetVedtak.getAktorId()));

        SendDokumentDTO sendDokumentDTO = lagDokumentDTO(utkastetVedtak, authKontekst.getFnr());
        OpprettetJournalpostDTO journalpost =
                dokumentServiceV2.produserOgJournalforDokument(sendDokumentDTO);

        String journalpostId = journalpost.getJournalpostId();
        String dokumentInfoId = null;
        if (journalpost.getDokumenter().isEmpty()) {
            log.error("Ingen dokumentInfoId i respons fra journalføring");
        } else {
            dokumentInfoId = journalpost.getDokumenter().get(0).getDokumentInfoId();
        }
        boolean journalpostferdigstilt = journalpost.getJournalpostferdigstilt();

        log.info(format(
                "Journalføring utført: journalpostId=%s, dokumentInfoId=%s, ferdigstilt=%s",
                journalpostId, dokumentInfoId, journalpostferdigstilt));

        vedtaksstotteRepository.lagreJournalforingVedtak(vedtakId, journalpostId, dokumentInfoId);

        if (!journalpostferdigstilt) {
            log.error("Journalpost ble ikke ferdigstilt. Må rettes manuelt.");
            metricsService.rapporterFeilendeFerdigstillingAvJournalpost();
        }

        transactor.executeWithoutResult(status -> {
            vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(utkastetVedtak.getAktorId());
            vedtaksstotteRepository.ferdigstillVedtakV2(vedtakId);
            beslutteroversiktRepository.slettBruker(utkastetVedtak.getId());
        });

        vedtakStatusEndringService.vedtakSendt(utkastetVedtak.getId(), Fnr.of(authKontekst.getFnr()));

        String bestillingsId = null;
        try {
            DistribuerJournalpostResponsDTO distribuerJournalpostResponsDTO =
                    dokumentServiceV2.distribuerJournalpost(journalpost.getJournalpostId());

            bestillingsId = distribuerJournalpostResponsDTO.getBestillingsId();
            log.info(format("Distribusjon av dokument bestilt: bestillingsId=%s", bestillingsId));
        } catch (RuntimeException e) {
            log.error("Distribusjon av journalpost feilet. Må rettes manuelt.", e);
            metricsService.rapporterFeilendeDistribusjonAvJournalpost();
        }

        if (bestillingsId != null) {
            vedtaksstotteRepository.lagreDokumentbestillingsId(vedtakId, bestillingsId);
        }

        return new DokumentSendtDTO(journalpostId, dokumentInfoId);
    }

    public BeslutterProsessStatus hentBeslutterprosessStatus(long vedtakId) {
        UtkastetVedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);
        authService.sjekkTilgangTilBrukerOgEnhet(AktorId.of(utkast.getAktorId()));
        return utkast.getBeslutterProsessStatus();
    }

    public UtkastetVedtak hentUtkast(Fnr fnr) {
        AuthKontekst authKontekst = authService.sjekkTilgangTilBrukerOgEnhet(fnr);
        String aktorId = authKontekst.getAktorId();
        UtkastetVedtak utkast = vedtaksstotteRepository.hentUtkast(aktorId);

        if (utkast == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bruker har ikke utkast");
        }

        flettInnVedtakInformasjon(utkast);

        return utkast;
    }

    public void lagUtkast(Fnr fnr) {
        AuthKontekst authKontekst = authService.sjekkTilgangTilBrukerOgEnhet(fnr);
        String aktorId = authKontekst.getAktorId();

        if (vedtaksstotteRepository.hentUtkast(aktorId) != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    format("Kan ikke lage nytt utkast, bruker med aktorId %s har allerede et aktivt utkast", aktorId)
            );
        }

        String innloggetVeilederIdent = authService.getInnloggetVeilederIdent();
        String oppfolgingsenhetId = authKontekst.getOppfolgingsenhet();

        vedtaksstotteRepository.opprettUtkast(aktorId, innloggetVeilederIdent, oppfolgingsenhetId);

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(aktorId);

        vedtakStatusEndringService.utkastOpprettet(utkast);
        meldingRepository.opprettSystemMelding(utkast.getId(), SystemMeldingType.UTKAST_OPPRETTET, innloggetVeilederIdent);
    }

    public void oppdaterUtkast(long vedtakId, OppdaterUtkastDTO vedtakDTO) {
        UtkastetVedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);
        authService.sjekkTilgangTilBrukerOgEnhet(AktorId.of(utkast.getAktorId()));
        authService.sjekkErAnsvarligVeilederFor(utkast);

        utkast = UtkastetVedtak.builder()
                .id(utkast.getId())
                .aktorId(utkast.getAktorId())
                .hovedmal(vedtakDTO.getInnsatsgruppe() == Innsatsgruppe.VARIG_TILPASSET_INNSATS ? null : vedtakDTO.getHovedmal())
                .innsatsgruppe(vedtakDTO.getInnsatsgruppe())
                .begrunnelse(vedtakDTO.getBegrunnelse())
                .veilederIdent(utkast.getVeilederIdent())
                .veilederNavn(utkast.getVeilederNavn())
                .oppfolgingsenhetId(utkast.getOppfolgingsenhetId())
                .oppfolgingsenhetNavn(utkast.getOppfolgingsenhetNavn())
                .beslutterIdent(utkast.getBeslutterIdent())
                .beslutterProsessStatus(utkast.getBeslutterProsessStatus())
                .opplysninger(vedtakDTO.getOpplysninger())
                .sistOppdatert(utkast.getSistOppdatert())
                .utkastOpprettet(utkast.getUtkastOpprettet())
                .vedtakStatus(utkast.getVedtakStatus())
                .build();

        List<String> utkastKilder = kilderRepository.hentKilderForVedtak(utkast.getId())
                .stream()
                .map(Kilde::getTekst)
                .collect(Collectors.toList());

        UtkastetVedtak finalUtkast = utkast;
        transactor.executeWithoutResult((status) -> {
            vedtaksstotteRepository.oppdaterUtkast(finalUtkast.getId(), finalUtkast);

            // Liten optimalisering for å slippe å slette og lage nye kilder når f.eks kun begrunnelse er endret
            if (!VedtakUtils.erKilderLike(utkastKilder, vedtakDTO.getOpplysninger())) {
                kilderRepository.slettKilder(finalUtkast.getId());
                kilderRepository.lagKilder(vedtakDTO.getOpplysninger(), finalUtkast.getId());
            }
        });
    }


    public void slettUtkastSomVeileder(long vedtakId) {
        UtkastetVedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);

        authService.sjekkTilgangTilBrukerOgEnhet(AktorId.of(utkast.getAktorId()));
        authService.sjekkErAnsvarligVeilederFor(utkast);

        slettUtkast(utkast);
    }

    public void slettUtkast(UtkastetVedtak utkast) {
        long utkastId = utkast.getId();

        transactor.executeWithoutResult((status) -> {
            meldingRepository.slettMeldinger(utkastId);
            beslutteroversiktRepository.slettBruker(utkastId);
            kilderRepository.slettKilder(utkastId);
            oyeblikksbildeService.slettOyeblikksbilde(utkastId); // Utkast skal i teorien ikke ha oyeblikksbilde, men hvis det oppstår en feilsituasjon så er det mulig
            vedtaksstotteRepository.slettUtkast(utkastId);
        });

        vedtakStatusEndringService.utkastSlettet(utkast);
    }

    public List<FattetVedtak> hentFattedeVedtak(Fnr fnr) {
        String aktorId = authService.sjekkTilgangTilBrukerOgEnhet(fnr).getAktorId();
        List<FattetVedtak> vedtak = vedtaksstotteRepository.hentFattedeVedtak(aktorId);

        vedtak.forEach(this::flettInnVedtakInformasjon);

        return vedtak;
    }

    private void flettInnVedtakInformasjon(Vedtak vedtak) {
        flettInnOpplysinger(vedtak);
        flettInnVeilederNavn(vedtak);
        flettInnBeslutterNavn(vedtak);
        flettInnEnhetNavn(vedtak);
    }

    public byte[] produserDokumentUtkast(long vedtakId) {
        Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);
        flettInnOpplysinger(utkast);
        AuthKontekst authKontekst = authService.sjekkTilgangTilBrukerOgEnhet(AktorId.of(utkast.getAktorId()));

        if (brukNyDokIntegrasjon()) {
            ProduserDokumentV2DTO produserDokumentV2DTO = lagProduserDokumentDTO(utkast, authKontekst.getFnr(), true);
            return dokumentClient.produserDokumentV2(produserDokumentV2DTO);
        } else {
            SendDokumentDTO sendDokumentDTO = lagDokumentDTO(utkast, authKontekst.getFnr());
            return dokumentClient.produserDokumentUtkast(sendDokumentDTO);
        }
    }

    public byte[] hentVedtakPdf(long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);
        if (vedtak == null || !(vedtak instanceof FattetVedtak)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke fattet vedtak");
        }

        FattetVedtak fattetVedtak = (FattetVedtak) vedtak;
        authService.sjekkTilgangTilBrukerOgEnhet(AktorId.of(vedtak.getAktorId()));
        return safClient.hentVedtakPdf(fattetVedtak.getJournalpostId(), fattetVedtak.getDokumentInfoId());
    }

    public boolean erFattet(long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);
        return (vedtak == null || !(vedtak instanceof FattetVedtak));
    }

    public boolean harUtkast(Fnr fnr) {
        String aktorId = authService.sjekkTilgangTilBrukerOgEnhet(fnr).getAktorId();
        return vedtaksstotteRepository.hentUtkast(aktorId) != null;
    }

    public void taOverUtkast(long vedtakId) {
        Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);

        authService.sjekkTilgangTilBrukerOgEnhet(AktorId.of(utkast.getAktorId()));

        String innloggetVeilederIdent = authService.getInnloggetVeilederIdent();
        Veileder veileder = veilederService.hentVeileder(innloggetVeilederIdent);

        if (innloggetVeilederIdent.equals(utkast.getVeilederIdent())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Veileder er allerede ansvarlig for utkast");
        }

        boolean erAlleredeBeslutter = innloggetVeilederIdent.equals(utkast.getBeslutterIdent());

        transactor.executeWithoutResult((status) -> {
            if (erAlleredeBeslutter) {
                vedtaksstotteRepository.setBeslutter(utkast.getId(), null);
            }
            vedtaksstotteRepository.oppdaterUtkastVeileder(utkast.getId(), innloggetVeilederIdent);
            beslutteroversiktRepository.oppdaterVeileder(utkast.getId(), veileder.getNavn());
            meldingRepository.opprettSystemMelding(utkast.getId(), SystemMeldingType.TATT_OVER_SOM_VEILEDER, innloggetVeilederIdent);
        });

        vedtakStatusEndringService.tattOverForVeileder(utkast, innloggetVeilederIdent);
    }

    private void flettInnOpplysinger(Vedtak vedtak) {
        List<String> opplysninger = kilderRepository
                .hentKilderForVedtak(vedtak.getId())
                .stream()
                .map(Kilde::getTekst)
                .collect(Collectors.toList());

        vedtak.setOpplysninger(opplysninger);
    }

    private void flettInnVeilederNavn(Vedtak vedtak) {
        Veileder veileder = veilederService.hentVeileder(vedtak.getVeilederIdent());
        vedtak.setVeilederNavn(veileder != null ? veileder.getNavn() : null);
    }

    private void flettInnBeslutterNavn(Vedtak vedtak) {
        if (vedtak.getBeslutterIdent() == null) {
            return;
        }

        Veileder beslutter = veilederService.hentVeileder(vedtak.getBeslutterIdent());
        vedtak.setBeslutterNavn(beslutter != null ? beslutter.getNavn() : null);
    }

    private void flettInnEnhetNavn(Vedtak vedtak) {
        String enhetNavn = veilederService.hentEnhetNavn(vedtak.getOppfolgingsenhetId());
        vedtak.setOppfolgingsenhetNavn(enhetNavn);
    }

    private SendDokumentDTO lagDokumentDTO(Vedtak vedtak, String fnr) {
        return new SendDokumentDTO(
                Fnr.of(fnr),
                malTypeService.utledMalTypeFraVedtak(vedtak, Fnr.of(fnr)),
                EnhetId.of(vedtak.getOppfolgingsenhetId()),
                vedtak.getBegrunnelse(),
                vedtak.getOpplysninger()
        );
    }

    private ProduserDokumentV2DTO lagProduserDokumentDTO(Vedtak vedtak, String fnr, boolean utkast) {
        return new ProduserDokumentV2DTO(
                Fnr.of(fnr),
                malTypeService.utledMalTypeFraVedtak(vedtak, Fnr.of(fnr)),
                EnhetId.of(vedtak.getOppfolgingsenhetId()),
                vedtak.getBegrunnelse(),
                vedtak.getOpplysninger(),
                utkast
        );
    }

    void validerVedtakForFerdigstillingOgUtsending(UtkastetVedtak vedtak, Vedtak gjeldendeVedtak) {

        if (vedtak.getVedtakStatus() != VedtakStatus.UTKAST) {
            throw new IllegalStateException("Vedtak har feil status, forventet status UTKAST");
        }

        Innsatsgruppe innsatsgruppe = vedtak.getInnsatsgruppe();

        if (innsatsgruppe == null) {
            throw new IllegalStateException("Vedtak mangler innsatsgruppe");
        }

        boolean isGodkjentAvBeslutter = vedtak.getBeslutterProsessStatus() == GODKJENT_AV_BESLUTTER;

        if (skalHaBeslutter(innsatsgruppe)) {
            if (vedtak.getBeslutterIdent() == null) {
                throw new IllegalStateException("Vedtak kan ikke bli sendt uten beslutter");
            } else if (!isGodkjentAvBeslutter) {
                throw new IllegalStateException("Vedtak er ikke godkjent av beslutter");
            }
        }

        if (vedtak.getOpplysninger() == null || vedtak.getOpplysninger().isEmpty()) {
            throw new IllegalStateException("Vedtak mangler opplysninger");
        }

        if (vedtak.getHovedmal() == null && innsatsgruppe != Innsatsgruppe.VARIG_TILPASSET_INNSATS) {
            throw new IllegalStateException("Vedtak mangler hovedmål");
        } else if (vedtak.getHovedmal() != null && innsatsgruppe == Innsatsgruppe.VARIG_TILPASSET_INNSATS) {
            throw new IllegalStateException("Vedtak med varig tilpasset innsats skal ikke ha hovedmål");
        }

        boolean harIkkeBegrunnelse = vedtak.getBegrunnelse() == null || vedtak.getBegrunnelse().trim().isEmpty();
        boolean erStandard = innsatsgruppe == Innsatsgruppe.STANDARD_INNSATS;
        boolean erGjeldendeVedtakVarig =
                gjeldendeVedtak != null &&
                        (gjeldendeVedtak.getInnsatsgruppe() == Innsatsgruppe.VARIG_TILPASSET_INNSATS ||
                                gjeldendeVedtak.getInnsatsgruppe() == Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS);

        if (harIkkeBegrunnelse && erStandard && erGjeldendeVedtakVarig) {
            throw new IllegalStateException("Vedtak mangler begrunnelse siden gjeldende vedtak er varig");
        } else if (harIkkeBegrunnelse && !erStandard) {
            throw new IllegalStateException("Vedtak mangler begrunnelse");
        }
    }

}
