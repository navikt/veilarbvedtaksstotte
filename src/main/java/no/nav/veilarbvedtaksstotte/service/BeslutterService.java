package no.nav.veilarbvedtaksstotte.service;

import no.nav.apiapp.feil.Feil;
import no.nav.apiapp.feil.UgyldigRequest;
import no.nav.veilarbvedtaksstotte.domain.*;
import no.nav.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.veilarbvedtaksstotte.utils.InnsatsgruppeUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

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
		AuthKontekst authKontekst = authService.sjekkTilgang(fnr);
		String aktorId = authKontekst.getAktorId();

		Vedtak vedtak = vedtaksstotteRepository.hentUtkastEllerFeil(aktorId);

		authService.sjekkAnsvarligVeileder(vedtak);

		if (!InnsatsgruppeUtils.skalHaBeslutter(vedtak.getInnsatsgruppe())) {
		    throw new UgyldigRequest();
        }

		if (!vedtak.isBeslutterProsessStartet()) {
			vedtaksstotteRepository.setBeslutterProsessStartet(vedtak.getId());
		}
	}

}
