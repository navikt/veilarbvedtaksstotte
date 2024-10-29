package no.nav.veilarbvedtaksstotte.service;

import io.getunleash.DefaultUnleash;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.dto.JournalpostGraphqlResponse;
import no.nav.veilarbvedtaksstotte.client.dokarkiv.request.OpprettetJournalpostDTO;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.Veileder;
import no.nav.veilarbvedtaksstotte.controller.dto.OppdaterUtkastDTO;
import no.nav.veilarbvedtaksstotte.domain.AuthKontekst;
import no.nav.veilarbvedtaksstotte.domain.arkiv.BrevKode;
import no.nav.veilarbvedtaksstotte.domain.dialog.SystemMeldingType;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeType;
import no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Kilde;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.domain.vedtak.VedtakStatus;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import no.nav.veilarbvedtaksstotte.repository.KilderRepository;
import no.nav.veilarbvedtaksstotte.repository.MeldingRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.veilarbvedtaksstotte.utils.VedtakUtils;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus.GODKJENT_AV_BESLUTTER;
import static no.nav.veilarbvedtaksstotte.domain.vedtak.VedtakStatus.SENDT;
import static no.nav.veilarbvedtaksstotte.utils.InnsatsgruppeUtils.skalHaBeslutter;

@Slf4j
@Service
@RequiredArgsConstructor
public class VedtakService {

    private final TransactionTemplate transactor;

    private final VedtaksstotteRepository vedtaksstotteRepository;
    private final BeslutteroversiktRepository beslutteroversiktRepository;
    private final KilderRepository kilderRepository;
    private final MeldingRepository meldingRepository;

    private final SafClient safClient;

    private final AuthService authService;

    private final OyeblikksbildeService oyeblikksbildeService;
    private final VeilederService veilederService;
    private final VedtakHendelserService vedtakStatusEndringService;
    private final DokumentService dokumentService;
    private final VeilarbarenaService veilarbarenaService;
    private final MetricsService metricsService;

    private final LeaderElectionClient leaderElection;

    @SneakyThrows
    public void fattVedtak(long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);
        AuthKontekst authKontekst = authService.sjekkTilgangTilBrukerOgEnhet(AktorId.of(vedtak.getAktorId()));
        authService.sjekkErAnsvarligVeilederFor(vedtak);

        if (veilarbarenaService.erBrukerInaktivIArena(Fnr.of(authKontekst.getFnr()))) {
            throw new IllegalStateException("Bruker kan ikke ha status ISERV når vedtak fattes");
        }

        flettInnVedtakInformasjon(vedtak);

        Vedtak gjeldendeVedtak = vedtaksstotteRepository.hentGjeldendeVedtak(vedtak.getAktorId());
        validerVedtakForFerdigstilling(vedtak, gjeldendeVedtak);

        ferdigstillVedtak(vedtak, gjeldendeVedtak);
    }

    private void ferdigstillVedtak(Vedtak vedtak, Vedtak gjeldendevedtak) {
        log.info(format("Ferdigstiller vedtak med id=%s ", vedtak.getId()));

        long vedtakId = vedtak.getId();

        UUID referanse = vedtaksstotteRepository.opprettOgHentReferanse(vedtak.getId());
        vedtak.setReferanse(referanse);

        log.info(format("Journalfører vedtak med id=%s og referanse=%s", vedtak.getId(), referanse));

        Fnr brukerFnr = authService.getFnrOrThrow(vedtak.getAktorId());

        oyeblikksbildeService.lagreOyeblikksbilde(brukerFnr.get(), vedtak.getId(), vedtak.getOpplysninger());

        journalforeVedtak(vedtak);

        transactor.executeWithoutResult(status -> {
            if (gjeldendevedtak != null) {
                vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(gjeldendevedtak.getId());
            }
            vedtaksstotteRepository.ferdigstillVedtak(vedtakId);
            beslutteroversiktRepository.slettBruker(vedtak.getId());
        });

        vedtakStatusEndringService.vedtakSendt(vedtak.getId());

        metricsService.rapporterMetrikkerForFattetVedtak(vedtak);
    }

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    public void journalforVedtak() {
        if (leaderElection.isLeader()) {
            List<Long> vedtakIds = vedtaksstotteRepository.hentVedtakForJournalforing(10);

            vedtakIds.forEach(vedtakId -> {
                log.info(String.format("SCHEDULED JOB: Journalfører vedtak med id: %s", vedtakId));
                Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);
                flettInnOpplysinger(vedtak);
                journalforeVedtak(vedtak);
            });
        }
    }

    public void journalforeVedtak(Vedtak vedtak) {
        try {
            Fnr brukerFnr = authService.getFnrOrThrow(vedtak.getAktorId());

            OpprettetJournalpostDTO journalpost =
                    dokumentService.produserOgJournalforDokumenterForVedtak(vedtak, brukerFnr);

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

            vedtaksstotteRepository.lagreJournalforingVedtak(vedtak.getId(), journalpostId, dokumentInfoId);

            if (!journalpostferdigstilt) {
                log.error(String.format("Journalpost ble ikke ferdigstilt. Må rettes manuelt. Vedtak id: %s", vedtak.getId()));
            }

            oppdatereDokumentIdforJournalfortOyeblikksbilde(vedtak.getId(), journalpostId);
        } catch (Exception e) {
            log.error(String.format("Kan ikke journalføre vedtak med id %s nå, feil: %s", vedtak.getId(), e.getMessage()), e);
        }
    }

    public BeslutterProsessStatus hentBeslutterprosessStatus(long vedtakId) {
        Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);
        authService.sjekkTilgangTilBrukerOgEnhet(AktorId.of(utkast.getAktorId()));
        return utkast.getBeslutterProsessStatus();
    }

    public Vedtak hentUtkast(Fnr fnr) {
        AuthKontekst authKontekst = authService.sjekkTilgangTilBrukerOgEnhet(fnr);
        String aktorId = authKontekst.getAktorId();
        Vedtak utkast = vedtaksstotteRepository.hentUtkast(aktorId);

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
                    HttpStatus.BAD_REQUEST, "Kan ikke lage utkast til bruker som allerede har et utkast"
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
        authService.sjekkTilgangTilBrukerOgEnhet(AktorId.of(utkast.getAktorId()));
        authService.sjekkErAnsvarligVeilederFor(utkast);

        oppdaterUtkastFraDto(utkast, vedtakDTO);

        List<String> utkastKilder = kilderRepository.hentKilderForVedtak(utkast.getId())
                .stream()
                .map(Kilde::getTekst)
                .collect(Collectors.toList());

        transactor.executeWithoutResult((status) -> {
            vedtaksstotteRepository.oppdaterUtkast(utkast.getId(), utkast);

            // Liten optimalisering for å slippe å slette og lage nye kilder når f.eks kun begrunnelse er endret
            if (!VedtakUtils.erKilderLike(utkastKilder, vedtakDTO.getOpplysninger())) {
                kilderRepository.slettKilder(utkast.getId());
                kilderRepository.lagKilder(vedtakDTO.getOpplysninger(), utkast.getId());
            }
        });
    }

    private void oppdatereDokumentIdforJournalfortOyeblikksbilde(long vedtakId, String journalpostId) {
        try {
            JournalpostGraphqlResponse journalpost = safClient.hentJournalpost(journalpostId);

            if (journalpost.getData().getJournalpost().dokumenter != null) {
                Arrays.stream(journalpost.getData().getJournalpost().dokumenter)
                        .filter(journalfortDokument -> OyeblikksbildeType.contains(journalfortDokument.brevkode))
                        .forEach(
                                journalfortDokument -> {
                                    oyeblikksbildeService.lagreJournalfortDokumentId(vedtakId, journalfortDokument.dokumentInfoId, OyeblikksbildeType.from(BrevKode.valueOf(journalfortDokument.brevkode)));
                                }
                        );
                log.info(format(
                        "Oppdatert dokumentId for oyeblikksbilde for vedtakId: %s",
                        vedtakId));
            }
        } catch (Exception e) {
            log.error("Feil med oppdatering av dokumentId for oyeblikksbilde " + e, e);
        }
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

        authService.sjekkTilgangTilBrukerOgEnhet(AktorId.of(utkast.getAktorId()));
        authService.sjekkErAnsvarligVeilederFor(utkast);

        slettUtkast(utkast);
    }

    public void slettUtkast(Vedtak utkast) {
        long utkastId = utkast.getId();

        transactor.executeWithoutResult((status) -> {
            meldingRepository.slettMeldinger(utkastId);
            beslutteroversiktRepository.slettBruker(utkastId);
            kilderRepository.slettKilder(utkastId);
            // Utkast skal i teorien ikke ha oyeblikksbilde, men hvis det oppstår en feilsituasjon så er det mulig
            oyeblikksbildeService.slettOyeblikksbilde(utkastId);
            vedtaksstotteRepository.slettUtkast(utkastId);
            vedtakStatusEndringService.utkastSlettet(utkast);
        });

        metricsService.rapporterUtkastSlettet();
    }

    public List<Vedtak> hentFattedeVedtak(Fnr fnr) {
        String aktorId = authService.sjekkTilgangTilBrukerOgEnhet(fnr).getAktorId();
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

        AuthKontekst authKontekst = authService.sjekkTilgangTilBrukerOgEnhet(AktorId.of(utkast.getAktorId()));

        flettInnOpplysinger(utkast);

        return dokumentService.produserDokumentutkast(utkast, Fnr.of(authKontekst.getFnr()));
    }

    public byte[] hentVedtakPdf(long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);
        if (vedtak == null || !SENDT.equals(vedtak.getVedtakStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke fattet vedtak");
        }
        authService.sjekkTilgangTilBrukerOgEnhet(AktorId.of(vedtak.getAktorId()));
        return safClient.hentVedtakPdf(vedtak.getJournalpostId(), vedtak.getDokumentInfoId());
    }

    public byte[] hentOyeblikksbildePdf(long vedtakId, String dokumentInfoId) {
        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);
        if (vedtak == null || !SENDT.equals(vedtak.getVedtakStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke fattet vedtak");
        }
        authService.sjekkTilgangTilBrukerOgEnhet(AktorId.of(vedtak.getAktorId()));
        return safClient.hentVedtakPdf(vedtak.getJournalpostId(), dokumentInfoId);
    }

    public boolean erFattet(long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);
        return vedtak != null && vedtak.getVedtakStatus() == SENDT;
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
        veilederService
                .hentVeilederEllerNull(vedtak.getVeilederIdent())
                .map(Veileder::getNavn)
                .ifPresent(vedtak::setVeilederNavn);
    }

    private void flettInnBeslutterNavn(Vedtak vedtak) {
        if (vedtak.getBeslutterIdent() == null) {
            return;
        }

        veilederService
                .hentVeilederEllerNull(vedtak.getBeslutterIdent())
                .map(Veileder::getNavn)
                .ifPresent(vedtak::setBeslutterNavn);
    }

    private void flettInnEnhetNavn(Vedtak vedtak) {
        String enhetNavn = veilederService.hentEnhetNavn(vedtak.getOppfolgingsenhetId());
        vedtak.setOppfolgingsenhetNavn(enhetNavn);
    }

    static void validerVedtakForFerdigstilling(Vedtak vedtak, Vedtak gjeldendeVedtak) {

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

        if (vedtak.getJournalpostId() != null ||
                vedtak.getDokumentInfoId() != null) {
            throw new IllegalStateException("Vedtak er allerede journalført");
        }
    }

}
