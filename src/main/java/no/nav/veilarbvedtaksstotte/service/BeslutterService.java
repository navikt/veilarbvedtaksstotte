package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.client.api.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.domain.BeslutteroversiktBruker;
import no.nav.veilarbvedtaksstotte.domain.PersonNavn;
import no.nav.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.veilarbvedtaksstotte.domain.Veileder;
import no.nav.veilarbvedtaksstotte.domain.dialog.SystemMeldingType;
import no.nav.veilarbvedtaksstotte.domain.enums.BeslutterProsessStatus;
import no.nav.veilarbvedtaksstotte.domain.enums.BeslutteroversiktStatus;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import no.nav.veilarbvedtaksstotte.repository.MeldingRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.veilarbvedtaksstotte.utils.EnumUtils;
import no.nav.veilarbvedtaksstotte.utils.InnsatsgruppeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import static no.nav.veilarbvedtaksstotte.utils.AutentiseringUtils.erAnsvarligVeilederForVedtak;
import static no.nav.veilarbvedtaksstotte.utils.AutentiseringUtils.erBeslutterForVedtak;
import static no.nav.veilarbvedtaksstotte.utils.VedtakUtils.erBeslutterProsessStartet;

@Service
public class BeslutterService {

	private final AuthService authService;

	private final VedtaksstotteRepository vedtaksstotteRepository;

	private final VedtakStatusEndringService vedtakStatusEndringService;

	private final BeslutteroversiktRepository beslutteroversiktRepository;

	private final MeldingRepository meldingRepository;

	private final VeilederService veilederService;

	private final VeilarbpersonClient veilarbpersonClient;

	private final TransactionTemplate transactor;

	@Autowired
	public BeslutterService(
			AuthService authService,
			VedtaksstotteRepository vedtaksstotteRepository,
			VedtakStatusEndringService vedtakStatusEndringService,
			BeslutteroversiktRepository beslutteroversiktRepository,
			MeldingRepository meldingRepository,
			VeilederService veilederService,
			VeilarbpersonClient veilarbpersonClient,
			TransactionTemplate transactor
	) {
		this.authService = authService;
		this.vedtaksstotteRepository = vedtaksstotteRepository;
		this.vedtakStatusEndringService = vedtakStatusEndringService;
		this.beslutteroversiktRepository = beslutteroversiktRepository;
		this.meldingRepository = meldingRepository;
		this.veilederService = veilederService;
		this.veilarbpersonClient = veilarbpersonClient;
		this.transactor = transactor;
	}

	public void startBeslutterProsess(long vedtakId) {
		Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);
		authService.sjekkTilgangTilAktorId(utkast.getAktorId());
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
	}

	public void avbrytBeslutterProsess(long vedtakId) {
		Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);
		authService.sjekkTilgangTilAktorId(utkast.getAktorId());
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
		});
	}

	public void bliBeslutter(long vedtakId) {
		Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);
		authService.sjekkTilgangTilAktorId(utkast.getAktorId());

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
	}

    public void setGodkjentAvBeslutter(long vedtakId) {
	    Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);
	    authService.sjekkTilgangTilAktorId(utkast.getAktorId());

        String innloggetVeilederIdent = authService.getInnloggetVeilederIdent();

        if (!erBeslutterForVedtak(innloggetVeilederIdent, utkast)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Kun beslutter kan godkjenne vedtak");
        }

        if (utkast.getBeslutterProsessStatus() == BeslutterProsessStatus.GODKJENT_AV_BESLUTTER) {
	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

		vedtaksstotteRepository.setBeslutterProsessStatus(utkast.getId(), BeslutterProsessStatus.GODKJENT_AV_BESLUTTER);
        beslutteroversiktRepository.oppdaterStatus(utkast.getId(), BeslutteroversiktStatus.GODKJENT_AV_BESLUTTER);
        vedtakStatusEndringService.godkjentAvBeslutter(utkast);
        meldingRepository.opprettSystemMelding(utkast.getId(), SystemMeldingType.BESLUTTER_HAR_GODKJENT, innloggetVeilederIdent);
    }

	public void oppdaterBeslutterProsessStatus(long vedtakId) {
		Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(vedtakId);
		authService.sjekkTilgangTilAktorId(utkast.getAktorId());

		String innloggetVeilederIdent = authService.getInnloggetVeilederIdent();
        BeslutterProsessStatus nyStatus;

		if (erBeslutterForVedtak(innloggetVeilederIdent, utkast)) {
		    nyStatus = BeslutterProsessStatus.KLAR_TIL_VEILEDER;
        } else if (erAnsvarligVeilederForVedtak(innloggetVeilederIdent, utkast)) {
		    nyStatus =  BeslutterProsessStatus.KLAR_TIL_BESLUTTER;
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
		String brukerFnr = authService.getFnrOrThrow(vedtak.getAktorId());
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
