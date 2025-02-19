package no.nav.veilarbvedtaksstotte.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.poao_tilgang.client.TilgangType;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.person.dto.PersonNavn;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.Veileder;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktBruker;
import no.nav.veilarbvedtaksstotte.domain.beslutteroversikt.BeslutteroversiktStatus;
import no.nav.veilarbvedtaksstotte.domain.dialog.SystemMeldingType;
import no.nav.veilarbvedtaksstotte.domain.vedtak.BeslutterProsessStatus;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import no.nav.veilarbvedtaksstotte.repository.MeldingRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.veilarbvedtaksstotte.utils.EnumUtils;
import no.nav.veilarbvedtaksstotte.utils.InnsatsgruppeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import static no.nav.veilarbvedtaksstotte.utils.AutentiseringUtils.erAnsvarligVeilederForVedtak;
import static no.nav.veilarbvedtaksstotte.utils.AutentiseringUtils.erBeslutterForVedtak;
import static no.nav.veilarbvedtaksstotte.utils.VedtakUtils.erBeslutterProsessStartet;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeslutterService {

	private final AuthService authService;

    private final VedtaksstotteRepository vedtaksstotteRepository;

    private final VedtakHendelserService vedtakStatusEndringService;

    private final BeslutteroversiktRepository beslutteroversiktRepository;

    private final MeldingRepository meldingRepository;

    private final VeilederService veilederService;

    private final VeilarbpersonClient veilarbpersonClient;

    private final TransactionTemplate transactor;

    private final MetricsService metricsService;

    private final SakStatistikkService sakStatistikkService;

    public void startBeslutterProsess(long vedtakId) {
        Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);
        authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, AktorId.of(utkast.getAktorId()));
        authService.sjekkErAnsvarligVeilederFor(utkast);

        if (!InnsatsgruppeUtils.skalHaBeslutter(utkast.getInnsatsgruppe())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        if (erBeslutterProsessStartet(utkast.getBeslutterProsessStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        leggTilBrukerIBeslutterOversikt(utkast);
        vedtaksstotteRepository.setBeslutterProsessStatus(utkast.getId(), BeslutterProsessStatus.KLAR_TIL_BESLUTTER);
        vedtakStatusEndringService.beslutterProsessStartet(utkast);
        meldingRepository.opprettSystemMelding(utkast.getId(), SystemMeldingType.BESLUTTER_PROSESS_STARTET, utkast.getVeilederIdent());
        sakStatistikkService.startetKvalitetssikring(utkast);
    }

    public void avbrytBeslutterProsess(long vedtakId) {
        Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);
        authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, AktorId.of(utkast.getAktorId()));
        authService.sjekkErAnsvarligVeilederFor(utkast);

        if (!erBeslutterProsessStartet(utkast.getBeslutterProsessStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        transactor.executeWithoutResult((status) -> {
            beslutteroversiktRepository.slettBruker(utkast.getId());
            vedtaksstotteRepository.setBeslutterProsessStatus(utkast.getId(), null);
            vedtaksstotteRepository.setBeslutter(utkast.getId(), null);
            vedtakStatusEndringService.beslutterProsessAvbrutt(utkast);
            meldingRepository.opprettSystemMelding(utkast.getId(), SystemMeldingType.BESLUTTER_PROSESS_AVBRUTT, utkast.getVeilederIdent());
            sakStatistikkService.avbrytKvalitetssikringsprosess(utkast);
        });
    }

    public void bliBeslutter(long vedtakId) {
        Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);
        authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, AktorId.of(utkast.getAktorId()));

        String innloggetVeilederIdent = authService.getInnloggetVeilederIdent();

        if (erAnsvarligVeilederForVedtak(innloggetVeilederIdent, utkast)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ansvarlig veileder kan ikke bli beslutter");
        }

        if (erBeslutterForVedtak(innloggetVeilederIdent, utkast)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        vedtaksstotteRepository.setBeslutter(utkast.getId(), innloggetVeilederIdent);

        Veileder beslutter = veilederService.hentVeileder(innloggetVeilederIdent);
        beslutteroversiktRepository.oppdaterBeslutter(utkast.getId(), beslutter.getIdent(), beslutter.getNavn());

        if (utkast.getBeslutterIdent() == null) {
            beslutteroversiktRepository.oppdaterStatus(utkast.getId(), BeslutteroversiktStatus.KLAR_TIL_BESLUTTER);
            vedtaksstotteRepository.setBeslutterProsessStatus(utkast.getId(), BeslutterProsessStatus.KLAR_TIL_BESLUTTER);
            vedtakStatusEndringService.blittBeslutter(utkast, innloggetVeilederIdent);
            meldingRepository.opprettSystemMelding(utkast.getId(), SystemMeldingType.BLITT_BESLUTTER, innloggetVeilederIdent);
        } else {
            vedtakStatusEndringService.tattOverForBeslutter(utkast, innloggetVeilederIdent);
            meldingRepository.opprettSystemMelding(utkast.getId(), SystemMeldingType.TATT_OVER_SOM_BESLUTTER, innloggetVeilederIdent);
        }

        sakStatistikkService.bliEllerTaOverSomKvalitetssikrer(utkast, innloggetVeilederIdent);
    }

    public void setGodkjentAvBeslutter(long vedtakId) {
        Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);
        authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, AktorId.of(utkast.getAktorId()));

        String innloggetVeilederIdent = authService.getInnloggetVeilederIdent();
        if (!erBeslutterForVedtak(innloggetVeilederIdent, utkast)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Kun beslutter kan godkjenne vedtak");
        }

        if (utkast.getBeslutterProsessStatus() == BeslutterProsessStatus.GODKJENT_AV_BESLUTTER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        vedtaksstotteRepository.setBeslutterProsessStatus(utkast.getId(), BeslutterProsessStatus.GODKJENT_AV_BESLUTTER);
        beslutteroversiktRepository.slettBruker(utkast.getId()); // Bruker skal ikke vises i beslutteroversikten hvis utkast er godkjent
        vedtakStatusEndringService.godkjentAvBeslutter(utkast);
        meldingRepository.opprettSystemMelding(utkast.getId(), SystemMeldingType.BESLUTTER_HAR_GODKJENT, innloggetVeilederIdent);
        metricsService.rapporterTidMellomUtkastOpprettetTilGodkjent(utkast);
        sakStatistikkService.kvalitetssikrerGodkjenner(utkast, innloggetVeilederIdent);
    }

    public void oppdaterBeslutterProsessStatus(long vedtakId) {
        Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);
        authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, AktorId.of(utkast.getAktorId()));

        String innloggetVeilederIdent = authService.getInnloggetVeilederIdent();
        BeslutterProsessStatus nyStatus;

        if (erBeslutterForVedtak(innloggetVeilederIdent, utkast)) {
            nyStatus = BeslutterProsessStatus.KLAR_TIL_VEILEDER;
        } else if (erAnsvarligVeilederForVedtak(innloggetVeilederIdent, utkast)) {
            nyStatus = BeslutterProsessStatus.KLAR_TIL_BESLUTTER;
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Kun ansvarlig veileder eller beslutter kan sette beslutter prosess status");
        }

        if (nyStatus == utkast.getBeslutterProsessStatus()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vedtak har allerede beslutter prosess status " + EnumUtils.getName(nyStatus));
        }

        vedtaksstotteRepository.setBeslutterProsessStatus(utkast.getId(), nyStatus);
        if (nyStatus == BeslutterProsessStatus.KLAR_TIL_BESLUTTER) {
            beslutteroversiktRepository.oppdaterStatus(utkast.getId(), BeslutteroversiktStatus.KLAR_TIL_BESLUTTER);
            meldingRepository.opprettSystemMelding(vedtakId, SystemMeldingType.SENDT_TIL_BESLUTTER, innloggetVeilederIdent);
            vedtakStatusEndringService.klarTilBeslutter(utkast);
        } else {
            beslutteroversiktRepository.oppdaterStatus(utkast.getId(), BeslutteroversiktStatus.KLAR_TIL_VEILEDER);
            meldingRepository.opprettSystemMelding(vedtakId, SystemMeldingType.SENDT_TIL_VEILEDER, innloggetVeilederIdent);
            vedtakStatusEndringService.klarTilVeileder(utkast);
        }
    }

    private void leggTilBrukerIBeslutterOversikt(Vedtak vedtak) {
        String brukerFnr = authService.getFnrOrThrow(vedtak.getAktorId()).get();
        Veileder veileder = veilederService.hentVeileder(vedtak.getVeilederIdent());
        String enhetNavn = veilederService.hentEnhetNavn(vedtak.getOppfolgingsenhetId());
        PersonNavn brukerNavn = veilarbpersonClient.hentPersonNavn(brukerFnr);

        BeslutteroversiktBruker bruker = new BeslutteroversiktBruker()
                .setVedtakId(vedtak.getId())
                .setBrukerFornavn(brukerNavn.getFornavn())
                .setBrukerEtternavn(brukerNavn.getEtternavn())
                .setBrukerFnr(brukerFnr)
                .setBrukerOppfolgingsenhetNavn(enhetNavn)
                .setBrukerOppfolgingsenhetId(vedtak.getOppfolgingsenhetId())
                .setVedtakStartet(vedtak.getUtkastOpprettet())
                .setStatus(BeslutteroversiktStatus.TRENGER_BESLUTTER)
                .setBeslutterNavn(null)
                .setBeslutterIdent(null)
                .setVeilederNavn(veileder.getNavn());

        beslutteroversiktRepository.lagBruker(bruker);
    }
}
