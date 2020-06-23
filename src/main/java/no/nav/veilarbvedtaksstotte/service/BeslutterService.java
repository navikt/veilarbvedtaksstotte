package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.client.api.PersonClient;
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

	private final PersonClient personClient;

	@Autowired
	public BeslutterService(
			AuthService authService,
			VedtaksstotteRepository vedtaksstotteRepository,
			VedtakStatusEndringService vedtakStatusEndringService,
			BeslutteroversiktRepository beslutteroversiktRepository,
			MeldingRepository meldingRepository,
			VeilederService veilederService,
			PersonClient personClient
	) {
		this.authService = authService;
		this.vedtaksstotteRepository = vedtaksstotteRepository;
		this.vedtakStatusEndringService = vedtakStatusEndringService;
		this.beslutteroversiktRepository = beslutteroversiktRepository;
		this.meldingRepository = meldingRepository;
		this.veilederService = veilederService;
		this.personClient = personClient;
	}

	public void startBeslutterProsess(String fnr) {
		String aktorId = authService.sjekkTilgangTilFnr(fnr).getAktorId();
		Vedtak vedtak = vedtaksstotteRepository.hentUtkastEllerFeil(aktorId);

		authService.sjekkAnsvarligVeileder(vedtak);

		if (!InnsatsgruppeUtils.skalHaBeslutter(vedtak.getInnsatsgruppe())) {
		    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

		if (erBeslutterProsessStartet(vedtak.getBeslutterProsessStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}

		leggTilBrukerIBeslutterOversikt(vedtak);
		vedtaksstotteRepository.setBeslutterProsessStatus(vedtak.getId(), BeslutterProsessStatus.KLAR_TIL_BESLUTTER);
		vedtakStatusEndringService.beslutterProsessStartet(vedtak);
		meldingRepository.opprettSystemMelding(vedtak.getId(), SystemMeldingType.BESLUTTER_PROSESS_STARTET, vedtak.getVeilederIdent());
	}

	public void bliBeslutter(String fnr) {
		String aktorId = authService.sjekkTilgangTilFnr(fnr).getAktorId();
		Vedtak vedtak = vedtaksstotteRepository.hentUtkastEllerFeil(aktorId);
		String innloggetVeilederIdent = authService.getInnloggetVeilederIdent();

		if (erAnsvarligVeilederForVedtak(innloggetVeilederIdent, vedtak)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ansvarlig veileder kan ikke bli beslutter");
		}

		if (erBeslutterForVedtak(innloggetVeilederIdent, vedtak)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}

		vedtaksstotteRepository.setBeslutter(vedtak.getId(), innloggetVeilederIdent);

		Veileder beslutter = veilederService.hentVeileder(innloggetVeilederIdent);
		beslutteroversiktRepository.oppdaterBeslutter(vedtak.getId(), beslutter.getIdent(), beslutter.getNavn());

		if (vedtak.getBeslutterIdent() == null) {
			beslutteroversiktRepository.oppdaterStatus(vedtak.getId(), BeslutteroversiktStatus.KLAR_TIL_BESLUTTER);
			vedtaksstotteRepository.setBeslutterProsessStatus(vedtak.getId(), BeslutterProsessStatus.KLAR_TIL_BESLUTTER);
			vedtakStatusEndringService.blittBeslutter(vedtak, innloggetVeilederIdent);
			meldingRepository.opprettSystemMelding(vedtak.getId(), SystemMeldingType.BLITT_BESLUTTER, innloggetVeilederIdent);
		} else {
			vedtakStatusEndringService.tattOverForBeslutter(vedtak, innloggetVeilederIdent);
			meldingRepository.opprettSystemMelding(vedtak.getId(), SystemMeldingType.TATT_OVER_SOM_BESLUTTER, innloggetVeilederIdent);
		}
	}

    public void setGodkjentAvBeslutter(String fnr) {
        String aktorId = authService.sjekkTilgangTilFnr(fnr).getAktorId();
        Vedtak vedtak = vedtaksstotteRepository.hentUtkastEllerFeil(aktorId);
        String innloggetVeilederIdent = authService.getInnloggetVeilederIdent();

        if (!erBeslutterForVedtak(innloggetVeilederIdent, vedtak)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Kun beslutter kan godkjenne vedtak");
        }

        if (vedtak.getBeslutterProsessStatus() == BeslutterProsessStatus.GODKJENT_AV_BESLUTTER) {
	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

		vedtaksstotteRepository.setBeslutterProsessStatus(vedtak.getId(), BeslutterProsessStatus.GODKJENT_AV_BESLUTTER);
        beslutteroversiktRepository.oppdaterStatus(vedtak.getId(), BeslutteroversiktStatus.GODKJENT_AV_BESLUTTER);
        vedtakStatusEndringService.godkjentAvBeslutter(vedtak);
        meldingRepository.opprettSystemMelding(vedtak.getId(), SystemMeldingType.BESLUTTER_HAR_GODKJENT, innloggetVeilederIdent);
    }

	public void oppdaterBeslutterProsessStatus(String fnr) {
		String aktorId = authService.sjekkTilgangTilFnr(fnr).getAktorId();
		Vedtak vedtak = vedtaksstotteRepository.hentUtkastEllerFeil(aktorId);
		String innloggetVeilederIdent = authService.getInnloggetVeilederIdent();
        BeslutterProsessStatus nyStatus;

		if (erBeslutterForVedtak(innloggetVeilederIdent, vedtak)) {
		    nyStatus = BeslutterProsessStatus.KLAR_TIL_VEILEDER;
        } else if (erAnsvarligVeilederForVedtak(innloggetVeilederIdent, vedtak)) {
		    nyStatus =  BeslutterProsessStatus.KLAR_TIL_BESLUTTER;
        } else {
		    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Kun ansvarlig veileder eller beslutter kan sette beslutter prosess status");
        }

		if (nyStatus == vedtak.getBeslutterProsessStatus()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vedtak har allerede beslutter prosess status " + EnumUtils.getName(nyStatus));
		}

		vedtaksstotteRepository.setBeslutterProsessStatus(vedtak.getId(), nyStatus);

		if (nyStatus == BeslutterProsessStatus.KLAR_TIL_BESLUTTER) {
			beslutteroversiktRepository.oppdaterStatus(vedtak.getId(), BeslutteroversiktStatus.KLAR_TIL_BESLUTTER);
			vedtakStatusEndringService.klarTilBeslutter(vedtak);
		} else {
			beslutteroversiktRepository.oppdaterStatus(vedtak.getId(), BeslutteroversiktStatus.KLAR_TIL_VEILEDER);
			vedtakStatusEndringService.klarTilVeileder(vedtak);
		}
	}

	private void leggTilBrukerIBeslutterOversikt(Vedtak vedtak) {
		String brukerFnr = authService.getFnrOrThrow(vedtak.getAktorId());
		Veileder veileder = veilederService.hentVeileder(vedtak.getVeilederIdent());
		String enhetNavn = veilederService.hentEnhetNavn(vedtak.getOppfolgingsenhetId());
		PersonNavn brukerNavn = personClient.hentPersonNavn(brukerFnr);

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
