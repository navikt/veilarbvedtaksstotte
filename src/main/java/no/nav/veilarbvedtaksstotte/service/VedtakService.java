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
import no.nav.veilarbvedtaksstotte.domain.BrukerIdenter;
import no.nav.veilarbvedtaksstotte.domain.dialog.SystemMeldingType;
import no.nav.veilarbvedtaksstotte.domain.vedtak.*;
import no.nav.veilarbvedtaksstotte.kafka.dto.KafkaOppfolgingsbrukerEndring;
import no.nav.veilarbvedtaksstotte.repository.*;
import no.nav.veilarbvedtaksstotte.utils.VedtakUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus.GODKJENT_AV_BESLUTTER;
import static no.nav.veilarbvedtaksstotte.domain.vedtak.VedtakStatus.SENDT;
import static no.nav.veilarbvedtaksstotte.utils.InnsatsgruppeUtils.skalHaBeslutter;

@Slf4j
@Service
public class VedtakService {

    private final TransactionTemplate transactor;

    private final VedtaksstotteRepository vedtaksstotteRepository;
    private final BeslutteroversiktRepository beslutteroversiktRepository;
    private final KilderRepository kilderRepository;
    private final MeldingRepository meldingRepository;
    private final ArenaVedtakRepository arenaVedtakRepository;

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
    private final BrukerIdentService brukerIdentService;

    @Autowired
    public VedtakService(
            TransactionTemplate transactor,

            VedtaksstotteRepository vedtaksstotteRepository,
            BeslutteroversiktRepository beslutteroversiktRepository,
            KilderRepository kilderRepository,
            MeldingRepository meldingRepository,
            ArenaVedtakRepository arenaVedtakRepository,

            VeilarbdokumentClient dokumentClient,
            SafClient safClient,

            AuthService authService,
            UnleashService unleashService,
            MetricsService metricsService,

            OyeblikksbildeService oyeblikksbildeService,
            VeilederService veilederService,
            MalTypeService malTypeService,
            VedtakStatusEndringService vedtakStatusEndringService,
            DokumentServiceV2 dokumentServiceV2,
            BrukerIdentService brukerIdentService
    ) {
        this.transactor = transactor;

        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.beslutteroversiktRepository = beslutteroversiktRepository;
        this.kilderRepository = kilderRepository;
        this.meldingRepository = meldingRepository;
        this.arenaVedtakRepository = arenaVedtakRepository;

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
        this.brukerIdentService = brukerIdentService;
    }

    @SneakyThrows
    public DokumentSendtDTO fattVedtak(long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);
        BrukerIdenter brukerIdenter = brukerIdentService.hentIdenter(AktorId.of(vedtak.getAktorId()));

        authService.sjekkTilgangTilBruker(brukerIdenter);
        authService.sjekkErAnsvarligVeilederFor(vedtak);

        flettInnVedtakInformasjon(vedtak);

        Vedtak gjeldendeVedtak = vedtaksstotteRepository.hentGjeldendeVedtak(vedtak.getAktorId());
        validerVedtakForFerdigstillingOgUtsending(vedtak, gjeldendeVedtak);

        return sendDokumentOgFerdigstill(vedtak, brukerIdenter);
    }

    private DokumentSendtDTO sendDokumentOgFerdigstill(Vedtak vedtak, BrukerIdenter brukerIdenter) {
        if (brukNyDokIntegrasjon()) {
            log.info(format("Sender og ferdigstiller vedtak med nye integrasjoner (vedtak id = %s, aktør id = %s)",
                    vedtak.getId(), brukerIdenter.getAktorId()));
            // Oppdaterer vedtak til "sender" tilstand for å redusere risiko for dupliserte utsendelser av dokument.
            vedtaksstotteRepository.oppdaterSender(vedtak.getId(), true);
            try {
                return sendDokumentOgFerdigstillV2(vedtak, brukerIdenter);
            } finally {
                vedtaksstotteRepository.oppdaterSender(vedtak.getId(), false);
            }
        } else {
            log.info(format("Sender og ferdigstiller vedtak med gammel integrasjon (vedtak id = %s, aktør id = %s)",
                    vedtak.getId(), brukerIdenter.getAktorId()));
            return sendDokumentOgFerdigstillV1(vedtak, brukerIdenter);
        }
    }

    private boolean brukNyDokIntegrasjon() {
        return EnvironmentUtils.isDevelopment().orElse(false) && unleashService.isNyDokIntegrasjonEnabled();
    }

    private DokumentSendtDTO sendDokumentOgFerdigstillV1(Vedtak vedtak, BrukerIdenter brukerIdenter) {
        oyeblikksbildeService.lagreOyeblikksbilde(brukerIdenter.getFnr().get(), vedtak.getId());

        DokumentSendtDTO dokumentSendt = sendDokument(vedtak, brukerIdenter.getFnr());

        log.info(String.format("Dokument sendt: journalpostId=%s dokumentId=%s",
                dokumentSendt.getJournalpostId(), dokumentSendt.getDokumentId()));

        transactor.executeWithoutResult((status) -> {
            vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(vedtak.getAktorId());
            vedtaksstotteRepository.ferdigstillVedtak(vedtak.getId(), dokumentSendt);
            beslutteroversiktRepository.slettBruker(vedtak.getId());
            slettArenaVedtakKopier(brukerIdenter);
        });

        vedtakStatusEndringService.vedtakSendt(vedtak.getId(), brukerIdenter.getFnr());

        return dokumentSendt;
    }

    private DokumentSendtDTO sendDokument(Vedtak vedtak, Fnr fnr) {
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

    private DokumentSendtDTO sendDokumentOgFerdigstillV2(Vedtak vedtak, BrukerIdenter brukerIdenter) {
        long vedtakId = vedtak.getId();

        oyeblikksbildeService.lagreOyeblikksbilde(brukerIdenter.getFnr().get(), vedtakId);

        log.info(format("Journalfører og distribuerer dokument for vedtak med id=%s for aktør id=%s",
                vedtakId, vedtak.getAktorId()));

        SendDokumentDTO sendDokumentDTO = lagDokumentDTO(vedtak, brukerIdenter.getFnr());
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
            vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(vedtak.getAktorId());
            vedtaksstotteRepository.ferdigstillVedtakV2(vedtakId);
            beslutteroversiktRepository.slettBruker(vedtak.getId());
            slettArenaVedtakKopier(brukerIdenter);
        });

        vedtakStatusEndringService.vedtakSendt(vedtak.getId(), brukerIdenter.getFnr());

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
        Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);
        authService.sjekkTilgangTilAktorId(utkast.getAktorId());
        return utkast.getBeslutterProsessStatus();
    }

    public Vedtak hentUtkast(String fnr) {
        AuthKontekst authKontekst = authService.sjekkTilgangTilFnr(fnr);
        String aktorId = authKontekst.getAktorId();
        Vedtak utkast = vedtaksstotteRepository.hentUtkast(aktorId);

        if (utkast == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bruker har ikke utkast");
        }

        flettInnVedtakInformasjon(utkast);

        return utkast;
    }

    public void lagUtkast(String fnr) {
        AuthKontekst authKontekst = authService.sjekkTilgangTilFnr(fnr);
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
        Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);
        authService.sjekkTilgangTilAktorId(utkast.getAktorId());
        authService.sjekkErAnsvarligVeilederFor(utkast);

        oppdaterUtkastFraDto(utkast, vedtakDTO);

        List<String> utkastKilder = kilderRepository.hentKilderForVedtak(utkast.getId())
                .stream()
                .map(Kilde::getTekst)
                .collect(toList());

        transactor.executeWithoutResult((status) -> {
            vedtaksstotteRepository.oppdaterUtkast(utkast.getId(), utkast);

            // Liten optimalisering for å slippe å slette og lage nye kilder når f.eks kun begrunnelse er endret
            if (!VedtakUtils.erKilderLike(utkastKilder, vedtakDTO.getOpplysninger())) {
                kilderRepository.slettKilder(utkast.getId());
                kilderRepository.lagKilder(vedtakDTO.getOpplysninger(), utkast.getId());
            }
        });
    }

    private void slettArenaVedtakKopier(BrukerIdenter brukerIdenter) {
        arenaVedtakRepository.slettVedtak(
                Stream.concat(brukerIdenter.getHistoriskeFnr().stream(), Stream.of(brukerIdenter.getFnr()))
                        .collect(toList())
        );
    }

    private void oppdaterUtkastFraDto(Vedtak utkast, OppdaterUtkastDTO dto) {
        utkast.setInnsatsgruppe(dto.getInnsatsgruppe());
        utkast.setBegrunnelse(dto.getBegrunnelse());
        utkast.setOpplysninger(dto.getOpplysninger());
        if (dto.getInnsatsgruppe() == Innsatsgruppe.VARIG_TILPASSET_INNSATS) {
            utkast.setHovedmal(null);
        } else {
            utkast.setHovedmal(dto.getHovedmal());
        }
    }

    public void slettUtkastSomVeileder(long vedtakId) {
        Vedtak utkast = vedtaksstotteRepository.hentVedtak(vedtakId);

        if (utkast.getVedtakStatus() != VedtakStatus.UTKAST) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kun utkast kan slettes");
        }

        authService.sjekkTilgangTilAktorId(utkast.getAktorId());
        authService.sjekkErAnsvarligVeilederFor(utkast);

        slettUtkast(utkast);
    }

    public void slettUtkast(Vedtak utkast) {
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

    public List<Vedtak> hentFattedeVedtak(String fnr) {
        String aktorId = authService.sjekkTilgangTilFnr(fnr).getAktorId();
        List<Vedtak> vedtak = vedtaksstotteRepository.hentFattedeVedtak(aktorId);

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
        AuthKontekst authKontekst = authService.sjekkTilgangTilAktorId(utkast.getAktorId());

        if (brukNyDokIntegrasjon()) {
            ProduserDokumentV2DTO produserDokumentV2DTO = lagProduserDokumentDTO(utkast, Fnr.of(authKontekst.getFnr()), true);
            return dokumentClient.produserDokumentV2(produserDokumentV2DTO);
        } else {
            SendDokumentDTO sendDokumentDTO = lagDokumentDTO(utkast, Fnr.of(authKontekst.getFnr()));
            return dokumentClient.produserDokumentUtkast(sendDokumentDTO);
        }
    }

    public byte[] hentVedtakPdf(long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);
        if (vedtak == null || !SENDT.equals(vedtak.getVedtakStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke fattet vedtak");
        }
        authService.sjekkTilgangTilAktorId(vedtak.getAktorId());
        return safClient.hentVedtakPdf(vedtak.getJournalpostId(), vedtak.getDokumentInfoId());
    }

    public boolean erFattet(long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);
        return vedtak != null && vedtak.getVedtakStatus() == SENDT;
    }

    public boolean harUtkast(String fnr) {
        String aktorId = authService.sjekkTilgangTilFnr(fnr).getAktorId();
        return vedtaksstotteRepository.hentUtkast(aktorId) != null;
    }

    public void behandleOppfolgingsbrukerEndring(KafkaOppfolgingsbrukerEndring endring) {
        Vedtak utkast = vedtaksstotteRepository.hentUtkast(endring.getAktorId());

        if (utkast != null && !utkast.getOppfolgingsenhetId().equals(endring.getOppfolgingsenhetId())) {
            vedtaksstotteRepository.oppdaterUtkastEnhet(utkast.getId(), endring.getOppfolgingsenhetId());
        }
    }

    public void taOverUtkast(long vedtakId) {
        Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);

        authService.sjekkTilgangTilAktorId(utkast.getAktorId());

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
                .collect(toList());

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

    private SendDokumentDTO lagDokumentDTO(Vedtak vedtak, Fnr fnr) {
        return new SendDokumentDTO(
                fnr,
                malTypeService.utledMalTypeFraVedtak(vedtak, fnr),
                EnhetId.of(vedtak.getOppfolgingsenhetId()),
                vedtak.getBegrunnelse(),
                vedtak.getOpplysninger()
        );
    }

    private ProduserDokumentV2DTO lagProduserDokumentDTO(Vedtak vedtak, Fnr fnr, boolean utkast) {
        return new ProduserDokumentV2DTO(
                fnr,
                malTypeService.utledMalTypeFraVedtak(vedtak, fnr),
                EnhetId.of(vedtak.getOppfolgingsenhetId()),
                vedtak.getBegrunnelse(),
                vedtak.getOpplysninger(),
                utkast
        );
    }

    void validerVedtakForFerdigstillingOgUtsending(Vedtak vedtak, Vedtak gjeldendeVedtak) {

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

        if (vedtak.getDokumentbestillingId() != null) {
            throw new IllegalStateException("Vedtak er allerede distribuert til bruker");
        }

        if (vedtak.getJournalpostId() != null ||
                vedtak.getDokumentInfoId() != null) {
            throw new IllegalStateException("Vedtak er allerede journalført");
        }

    }

}
