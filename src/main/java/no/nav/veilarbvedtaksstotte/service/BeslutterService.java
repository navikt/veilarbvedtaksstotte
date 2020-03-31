package no.nav.veilarbvedtaksstotte.service;

import no.nav.apiapp.feil.Feil;
import no.nav.apiapp.feil.FeilType;
import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.feil.UgyldigRequest;
import no.nav.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.veilarbvedtaksstotte.domain.enums.BeslutterProsessStatus;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.veilarbvedtaksstotte.utils.EnumUtils;
import no.nav.veilarbvedtaksstotte.utils.InnsatsgruppeUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import static no.nav.veilarbvedtaksstotte.utils.AutentiseringUtils.erAnsvarligVeilederForVedtak;
import static no.nav.veilarbvedtaksstotte.utils.AutentiseringUtils.erBeslutterForVedtak;

@Service
public class BeslutterService {

	private final AuthService authService;

	private final VedtaksstotteRepository vedtaksstotteRepository;

	private final VedtakStatusEndringService vedtakStatusEndringService;

	@Inject
	public BeslutterService(AuthService authService, VedtaksstotteRepository vedtaksstotteRepository, VedtakStatusEndringService vedtakStatusEndringService) {
		this.authService = authService;
		this.vedtaksstotteRepository = vedtaksstotteRepository;
		this.vedtakStatusEndringService = vedtakStatusEndringService;
	}

	public void startBeslutterProsess(String fnr) {
		String aktorId = authService.sjekkTilgang(fnr).getAktorId();
		Vedtak vedtak = vedtaksstotteRepository.hentUtkastEllerFeil(aktorId);

		authService.sjekkAnsvarligVeileder(vedtak);

		if (!InnsatsgruppeUtils.skalHaBeslutter(vedtak.getInnsatsgruppe())) {
		    throw new UgyldigRequest();
        }

		if (vedtak.isBeslutterProsessStartet()) {
			throw new UgyldigRequest();
		}

		vedtaksstotteRepository.setBeslutterProsessStartet(vedtak.getId());
		vedtakStatusEndringService.beslutterProsessStartet(vedtak);
	}

	public void bliBeslutter(String fnr) {
		String aktorId = authService.sjekkTilgang(fnr).getAktorId();
		Vedtak vedtak = vedtaksstotteRepository.hentUtkastEllerFeil(aktorId);
		String innloggetVeilederIdent = authService.getInnloggetVeilederIdent();

		if (erAnsvarligVeilederForVedtak(innloggetVeilederIdent, vedtak)) {
			throw new IngenTilgang("Ansvarlig veileder kan ikke bli beslutter");
		}

		if (erBeslutterForVedtak(innloggetVeilederIdent, vedtak)) {
			throw new UgyldigRequest();
		}

		vedtaksstotteRepository.setBeslutter(vedtak.getId(), innloggetVeilederIdent);

		if (vedtak.getBeslutterIdent() == null) {
			vedtakStatusEndringService.blittBeslutter(vedtak, innloggetVeilederIdent);
		} else {
			vedtakStatusEndringService.tattOverForBeslutter(vedtak, innloggetVeilederIdent);
		}
	}

    public void setGodkjentAvBeslutter(String fnr) {
        String aktorId = authService.sjekkTilgang(fnr).getAktorId();
        Vedtak vedtak = vedtaksstotteRepository.hentUtkastEllerFeil(aktorId);
        String innloggetVeilederIdent = authService.getInnloggetVeilederIdent();

        if (!erBeslutterForVedtak(innloggetVeilederIdent, vedtak)) {
            throw new IngenTilgang("Kun beslutter kan godkjenne vedtak");
        }

        if (vedtak.isGodkjentAvBeslutter()) {
			throw new UgyldigRequest();
        }

		vedtaksstotteRepository.setGodkjentAvBeslutter(vedtak.getId(), true);
        vedtakStatusEndringService.godkjentAvBeslutter(vedtak);
    }

	public void oppdaterBeslutterProsessStatus(String fnr) {
		String aktorId = authService.sjekkTilgang(fnr).getAktorId();
		Vedtak vedtak = vedtaksstotteRepository.hentUtkastEllerFeil(aktorId);
		String innloggetVeilederIdent = authService.getInnloggetVeilederIdent();
        BeslutterProsessStatus nyStatus;

		if (erBeslutterForVedtak(innloggetVeilederIdent, vedtak)) {
		    nyStatus = BeslutterProsessStatus.KLAR_TIL_VEILEDER;
        } else if (erAnsvarligVeilederForVedtak(innloggetVeilederIdent, vedtak)) {
		    nyStatus =  BeslutterProsessStatus.KLAR_TIL_BESLUTTER;
        } else {
		    throw new IngenTilgang("Kun ansvarlig veileder eller beslutter kan sette beslutter prosess status");
        }

		if (nyStatus == vedtak.getBeslutterProsessStatus()) {
			throw new Feil(FeilType.UGYLDIG_REQUEST, "Vedtak har allerede beslutter prosess status " + EnumUtils.getName(nyStatus));
		}

		vedtaksstotteRepository.setBeslutterProsessStatus(vedtak.getId(), nyStatus);

		if (nyStatus == BeslutterProsessStatus.KLAR_TIL_BESLUTTER) {
			vedtakStatusEndringService.klarTilBeslutter(vedtak);
		} else {
			vedtakStatusEndringService.klarTilVeileder(vedtak);
		}
	}

}
