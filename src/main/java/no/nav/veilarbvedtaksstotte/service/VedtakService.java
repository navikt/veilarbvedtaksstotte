package no.nav.veilarbvedtaksstotte.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.client.api.dokument.DokumentClient;
import no.nav.veilarbvedtaksstotte.client.api.dokarkiv.SafClient;
import no.nav.veilarbvedtaksstotte.client.api.dokument.DokumentSendtDTO;
import no.nav.veilarbvedtaksstotte.client.api.dokument.SendDokumentDTO;
import no.nav.veilarbvedtaksstotte.client.api.veilederogenhet.Veileder;
import no.nav.veilarbvedtaksstotte.domain.*;
import no.nav.veilarbvedtaksstotte.domain.dialog.SystemMeldingType;
import no.nav.veilarbvedtaksstotte.domain.enums.BeslutterProsessStatus;
import no.nav.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.domain.enums.VedtakStatus;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import no.nav.veilarbvedtaksstotte.repository.KilderRepository;
import no.nav.veilarbvedtaksstotte.repository.MeldingRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static no.nav.veilarbvedtaksstotte.domain.enums.BeslutterProsessStatus.GODKJENT_AV_BESLUTTER;
import static no.nav.veilarbvedtaksstotte.domain.enums.VedtakStatus.SENDT;
import static no.nav.veilarbvedtaksstotte.utils.InnsatsgruppeUtils.skalHaBeslutter;

@Slf4j
@Service
public class VedtakService {

    private final VedtaksstotteRepository vedtaksstotteRepository;
    private final OyeblikksbildeService oyeblikksbildeService;
    private final KilderRepository kilderRepository;
    private final MeldingRepository meldingRepository;
    private final BeslutteroversiktRepository beslutteroversiktRepository;
    private final AuthService authService;
    private final DokumentClient dokumentClient;
    private final SafClient safClient;
    private final VeilederService veilederService;
    private final MalTypeService malTypeService;
    private final VedtakStatusEndringService vedtakStatusEndringService;
    private final MetricsService metricsService;
    private final TransactionTemplate transactor;

    @Autowired
    public VedtakService(
            VedtaksstotteRepository vedtaksstotteRepository,
            KilderRepository kilderRepository,
            OyeblikksbildeService oyeblikksbildeService,
            MeldingRepository meldingRepository,
            BeslutteroversiktRepository beslutteroversiktRepository,
            AuthService authService,
            DokumentClient dokumentClient,
            SafClient safClient,
            VeilederService veilederService,
            MalTypeService malTypeService,
            VedtakStatusEndringService vedtakStatusEndringService,
            MetricsService metricsService,
            TransactionTemplate transactor
    ) {
        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.kilderRepository = kilderRepository;
        this.oyeblikksbildeService = oyeblikksbildeService;
        this.meldingRepository = meldingRepository;
        this.beslutteroversiktRepository = beslutteroversiktRepository;
        this.authService = authService;
        this.dokumentClient = dokumentClient;
        this.safClient = safClient;
        this.veilederService = veilederService;
        this.malTypeService = malTypeService;
        this.vedtakStatusEndringService = vedtakStatusEndringService;
        this.metricsService = metricsService;
        this.transactor = transactor;
    }

    @SneakyThrows
    public DokumentSendtDTO fattVedtak(long vedtakId) {
        Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);
        AuthKontekst authKontekst = authService.sjekkTilgangTilAktorId(utkast.getAktorId());
        authService.sjekkErAnsvarligVeilederFor(utkast);

        flettInnVedtakInformasjon(utkast);

        Vedtak gjeldendeVedtak = vedtaksstotteRepository.hentGjeldendeVedtak(utkast.getAktorId());
        validerUtkastForUtsending(utkast, gjeldendeVedtak);

        oyeblikksbildeService.lagreOyeblikksbilde(authKontekst.getFnr(), vedtakId);

        DokumentSendtDTO dokumentSendt = sendDokument(utkast, authKontekst.getFnr());

        log.info(String.format("Dokument sendt: journalpostId=%s dokumentId=%s",
                dokumentSendt.getJournalpostId(), dokumentSendt.getDokumentId()));

        transactor.executeWithoutResult((status) -> {
            vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(utkast.getAktorId());
            vedtaksstotteRepository.ferdigstillVedtak(vedtakId, dokumentSendt);
            beslutteroversiktRepository.slettBruker(vedtakId);
        });

        vedtakStatusEndringService.vedtakSendt(utkast, authKontekst.getFnr());

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
        Vedtak utkast = vedtaksstotteRepository.hentUtkast(aktorId);

        if (utkast != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, format("Kan ikke lage nytt utkast, bruker med aktorId %s har allerede et aktivt utkast", aktorId));
        }

        String innloggetVeilederIdent = authService.getInnloggetVeilederIdent();
        String oppfolgingsenhetId = authKontekst.getOppfolgingsenhet();

        vedtaksstotteRepository.opprettUtkast(aktorId, innloggetVeilederIdent, oppfolgingsenhetId);
        vedtakStatusEndringService.utkastOpprettet(vedtaksstotteRepository.hentUtkast(aktorId));

        Vedtak nyttUtkast = vedtaksstotteRepository.hentUtkast(aktorId);
        meldingRepository.opprettSystemMelding(nyttUtkast.getId(), SystemMeldingType.UTKAST_OPPRETTET, innloggetVeilederIdent);
    }

    public void oppdaterUtkast(long vedtakId, VedtakDTO vedtakDTO) {
        Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);
        authService.sjekkTilgangTilAktorId(utkast.getAktorId());
        authService.sjekkErAnsvarligVeilederFor(utkast);

        oppdaterUtkastFraDto(utkast, vedtakDTO);

        // TODO: Kan vurdere å sjekke hvilke repos som trengs å kalles basert på endringen som er gjort

        transactor.executeWithoutResult((status) -> {
            vedtaksstotteRepository.oppdaterUtkast(utkast.getId(), utkast);
            kilderRepository.slettKilder(utkast.getId());
            kilderRepository.lagKilder(vedtakDTO.getOpplysninger(), utkast.getId());
        });
    }

    private void oppdaterUtkastFraDto(Vedtak utkast, VedtakDTO dto) {
        utkast.setInnsatsgruppe(dto.getInnsatsgruppe());
        utkast.setBegrunnelse(dto.getBegrunnelse());
        utkast.setOpplysninger(dto.getOpplysninger());
        if (dto.getInnsatsgruppe() == Innsatsgruppe.VARIG_TILPASSET_INNSATS) {
            utkast.setHovedmal(null);
        } else {
            utkast.setHovedmal(dto.getHovedmal());
        }
    }

    public void slettUtkast(long vedtakId) {
        Vedtak utkast = vedtaksstotteRepository.hentVedtak(vedtakId);

        if (utkast.getVedtakStatus() != VedtakStatus.UTKAST) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kun utkast kan slettes");
        }

        authService.sjekkTilgangTilAktorId(utkast.getAktorId());
        slettUtkast(utkast.getAktorId());
    }

    public void slettUtkast(String aktorId) {
        Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(aktorId);
        long utkastId = utkast.getId();
        authService.sjekkErAnsvarligVeilederFor(utkast);

        transactor.executeWithoutResult((status) -> {
            meldingRepository.slettMeldinger(utkastId);
            kilderRepository.slettKilder(utkastId);
            beslutteroversiktRepository.slettBruker(utkastId);
            kilderRepository.slettKilder(utkastId);
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
        SendDokumentDTO sendDokumentDTO = lagDokumentDTO(utkast, authKontekst.getFnr());
        return dokumentClient.produserDokumentUtkast(sendDokumentDTO);
    }

    public byte[] hentVedtakPdf(long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);
        if (vedtak == null || !SENDT.equals(vedtak.getVedtakStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke fattet vedtak");
        }
        authService.sjekkTilgangTilAktorId(vedtak.getAktorId());
        return safClient.hentVedtakPdf(vedtak.getJournalpostId(), vedtak.getDokumentInfoId());
    }

    public boolean harUtkast(String fnr) {
        String aktorId = authService.sjekkTilgangTilFnr(fnr).getAktorId();
        return vedtaksstotteRepository.hentUtkast(aktorId) != null;
    }

    public void behandleAvsluttOppfolging(KafkaAvsluttOppfolging melding) {
        if (vedtaksstotteRepository.hentUtkast(melding.getAktorId()) != null) {
            slettUtkast(melding.getAktorId());
        }
        vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(melding.getAktorId());
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

        vedtaksstotteRepository.oppdaterUtkastVeileder(utkast.getId(), innloggetVeilederIdent);
        beslutteroversiktRepository.oppdaterVeileder(utkast.getId(), veileder.getNavn());
        vedtakStatusEndringService.tattOverForVeileder(utkast, innloggetVeilederIdent);
        meldingRepository.opprettSystemMelding(utkast.getId(), SystemMeldingType.TATT_OVER_SOM_VEILEDER, innloggetVeilederIdent);
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
        return new SendDokumentDTO()
                .setBegrunnelse(vedtak.getBegrunnelse())
                .setEnhetId(vedtak.getOppfolgingsenhetId())
                .setOpplysninger(vedtak.getOpplysninger())
                .setMalType(malTypeService.utledMalTypeFraVedtak(vedtak, fnr))
                .setBrukerFnr(fnr);
    }

    void validerUtkastForUtsending(Vedtak vedtak, Vedtak gjeldendeVedtak) {

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
