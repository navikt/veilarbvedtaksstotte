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

	private AuthService authService;
	private VedtaksstotteRepository vedtaksstotteRepository;

	@Inject
	public BeslutterService(AuthService authService, VedtaksstotteRepository vedtaksstotteRepository) {
		this.authService = authService;
		this.vedtaksstotteRepository = vedtaksstotteRepository;
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
    }

	public void oppdaterBeslutterProsessStatus(String fnr) {
		String aktorId = authService.sjekkTilgang(fnr).getAktorId();
		Vedtak vedtak = vedtaksstotteRepository.hentUtkastEllerFeil(aktorId);
		String innloggetVeilederIdent = authService.getInnloggetVeilederIdent();

		BeslutterProsessStatus nyStatus = erBeslutterForVedtak(innloggetVeilederIdent, vedtak)
				? BeslutterProsessStatus.KLAR_TIL_VEILEDER
				: BeslutterProsessStatus.KLAR_TIL_BESLUTTER;

		if (nyStatus == vedtak.getBeslutterProsessStatus()) {
			throw new Feil(FeilType.UGYLDIG_REQUEST, "Vedtak har allerede beslutter prosess status " + EnumUtils.getName(nyStatus));
		}

		vedtaksstotteRepository.setBeslutterProsessStatus(vedtak.getId(), nyStatus);
	}

}
