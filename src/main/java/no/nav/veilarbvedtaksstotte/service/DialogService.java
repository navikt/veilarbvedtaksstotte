package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.domain.DialogMelding;
import no.nav.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.DialogRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

@Service
public class DialogService {

    private final AuthService authService;
    private final DialogRepository dialogRepository;
    private final VedtaksstotteRepository vedtaksstotteRepository;

    @Inject
    public DialogService(AuthService authService, DialogRepository dialogRepository, VedtaksstotteRepository vedtaksstotteRepository) {
        this.authService = authService;
        this.dialogRepository = dialogRepository;
        this.vedtaksstotteRepository = vedtaksstotteRepository;
    }

    public void opprettBrukerDialogMelding(String fnr, String melding) {
        String aktorId = authService.sjekkTilgang(fnr).getAktorId();
        String innloggetVeilederIdent = authService.getInnloggetVeilederIdent();
        Vedtak vedtak = vedtaksstotteRepository.hentUtkastEllerFeil(aktorId);
        dialogRepository.opprettDialogMelding(vedtak.getId(), innloggetVeilederIdent, melding);
    }

    public List<DialogMelding> hentDialogMeldinger(String fnr) {
        String aktorId = authService.sjekkTilgang(fnr).getAktorId();
        Vedtak vedtak = vedtaksstotteRepository.hentUtkastEllerFeil(aktorId);
        return dialogRepository.hentDialogMeldinger(vedtak.getId());
    }

}
