package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.domain.DialogMeldingDTO;
import no.nav.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.veilarbvedtaksstotte.domain.Veileder;
import no.nav.veilarbvedtaksstotte.repository.DialogRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DialogService {

    private final AuthService authService;
    private final VeilederService veilederService;
    private final DialogRepository dialogRepository;
    private final VedtaksstotteRepository vedtaksstotteRepository;

    @Inject
    public DialogService(
            AuthService authService, VeilederService veilederService,
            DialogRepository dialogRepository, VedtaksstotteRepository vedtaksstotteRepository
    ) {
        this.authService = authService;
        this.veilederService = veilederService;
        this.dialogRepository = dialogRepository;
        this.vedtaksstotteRepository = vedtaksstotteRepository;
    }

    public void opprettBrukerDialogMelding(String fnr, String melding) {
        String aktorId = authService.sjekkTilgang(fnr).getAktorId();
        String innloggetVeilederIdent = authService.getInnloggetVeilederIdent();
        Vedtak vedtak = vedtaksstotteRepository.hentUtkastEllerFeil(aktorId);
        dialogRepository.opprettDialogMelding(vedtak.getId(), innloggetVeilederIdent, melding);
    }

    public List<DialogMeldingDTO> hentDialogMeldinger(String fnr) {
        String aktorId = authService.sjekkTilgang(fnr).getAktorId();
        Vedtak vedtak = vedtaksstotteRepository.hentUtkastEllerFeil(aktorId);

        List<DialogMeldingDTO> meldinger = dialogRepository.hentDialogMeldinger(vedtak.getId())
                .stream()
                .map(DialogMeldingDTO::fraMelding)
                .collect(Collectors.toList());

        flettInnNavn(meldinger);

        return meldinger;
    }

    private void flettInnNavn(List<DialogMeldingDTO> meldinger) {
        meldinger.forEach(melding -> {
            Veileder veileder = veilederService.hentVeileder(melding.getOpprettetAvIdent());
            melding.setOpprettetAvNavn(veileder.getNavn());
        });
    }

}
