package no.nav.veilarbvedtaksstotte.service;

import no.nav.apiapp.feil.IngenTilgang;
import no.nav.apiapp.feil.UgyldigRequest;
import no.nav.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
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

		if (!vedtak.isBeslutterProsessStartet()) {
			vedtaksstotteRepository.setBeslutterProsessStartet(vedtak.getId());
		}
	}

	public void bliBeslutter(String fnr) {
		String aktorId = authService.sjekkTilgang(fnr).getAktorId();
		Vedtak vedtak = vedtaksstotteRepository.hentUtkastEllerFeil(aktorId);
		String innloggetVeilederIdent = authService.getInnloggetVeilederIdent();

		if (erAnsvarligVeilederForVedtak(innloggetVeilederIdent, vedtak)) {
			throw new IngenTilgang("Ansvarlig veileder kan ikke bli beslutter");
		}

		if (!erBeslutterForVedtak(innloggetVeilederIdent, vedtak)) {
			vedtaksstotteRepository.setBeslutter(vedtak.getId(), innloggetVeilederIdent);
		}
	}

    public void setGodkjentAvBeslutter(String fnr) {
        String aktorId = authService.sjekkTilgang(fnr).getAktorId();
        Vedtak vedtak = vedtaksstotteRepository.hentUtkastEllerFeil(aktorId);
        String innloggetVeilederIdent = authService.getInnloggetVeilederIdent();

        if (!erBeslutterForVedtak(innloggetVeilederIdent, vedtak)) {
            throw new IngenTilgang("Kun beslutter kan godkjenne vedtak");
        }

        if (!vedtak.isGodkjentAvBeslutter()) {
            vedtaksstotteRepository.setGodkjentAvBeslutter(vedtak.getId(), true);
        }
    }

}
