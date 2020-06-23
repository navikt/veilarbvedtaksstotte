package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.veilarbvedtaksstotte.domain.Veileder;
import no.nav.veilarbvedtaksstotte.domain.dialog.DialogMeldingDTO;
import no.nav.veilarbvedtaksstotte.domain.dialog.MeldingDTO;
import no.nav.veilarbvedtaksstotte.domain.dialog.SystemMeldingDTO;
import no.nav.veilarbvedtaksstotte.repository.MeldingRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.nav.veilarbvedtaksstotte.utils.AutentiseringUtils.erAnsvarligVeilederForVedtak;
import static no.nav.veilarbvedtaksstotte.utils.AutentiseringUtils.erBeslutterForVedtak;

@Service
public class MeldingService {

    private final AuthService authService;
    private final VeilederService veilederService;
    private final MeldingRepository meldingRepository;
    private final VedtaksstotteRepository vedtaksstotteRepository;
    private final MetricsService metricsService;

    @Autowired
    public MeldingService(
            AuthService authService, VeilederService veilederService,
            MeldingRepository meldingRepository, VedtaksstotteRepository vedtaksstotteRepository,
            MetricsService metricsService) {
        this.authService = authService;
        this.veilederService = veilederService;
        this.meldingRepository = meldingRepository;
        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.metricsService = metricsService;
    }

    public void opprettBrukerDialogMelding(String fnr, String melding) {
        String aktorId = authService.sjekkTilgangTilFnr(fnr).getAktorId();
        String innloggetVeilederIdent = authService.getInnloggetVeilederIdent();
        Vedtak vedtak = vedtaksstotteRepository.hentUtkastEllerFeil(aktorId);
        meldingRepository.opprettDialogMelding(vedtak.getId(), innloggetVeilederIdent, melding);

        if (erBeslutterForVedtak(innloggetVeilederIdent, vedtak)) {
            metricsService.repporterDialogMeldingSendtAvVeilederOgBeslutter(melding, "beslutter");
        } else if (erAnsvarligVeilederForVedtak(innloggetVeilederIdent, vedtak)) {
            metricsService.repporterDialogMeldingSendtAvVeilederOgBeslutter(melding, "veileder");
        }
    }

    public List<MeldingDTO> hentMeldinger(String fnr) {
        String aktorId = authService.sjekkTilgangTilFnr(fnr).getAktorId();
        Vedtak vedtak = vedtaksstotteRepository.hentUtkastEllerFeil(aktorId);

        List<DialogMeldingDTO> dialogMeldinger = hentDialogMeldinger(vedtak.getId());
        List<SystemMeldingDTO> systemMeldinger = hentSystemMeldinger(vedtak.getId());

        return Stream.concat(dialogMeldinger.stream(), systemMeldinger.stream())
                .collect(Collectors.toList());
    }

    private List<SystemMeldingDTO> hentSystemMeldinger(long vedtakId) {
        List<SystemMeldingDTO> meldinger = meldingRepository.hentSystemMeldinger(vedtakId)
                .stream()
                .map(SystemMeldingDTO::fraMelding)
                .collect(Collectors.toList());

        meldinger.forEach(melding -> {
            Veileder veileder = veilederService.hentVeileder(melding.getUtfortAvIdent());
            melding.setUtfortAvNavn(veileder.getNavn());
        });

        return meldinger;
    }

    private List<DialogMeldingDTO> hentDialogMeldinger(long vedtakId) {
        List<DialogMeldingDTO> meldinger = meldingRepository.hentDialogMeldinger(vedtakId)
                .stream()
                .map(DialogMeldingDTO::fraMelding)
                .collect(Collectors.toList());

        meldinger.forEach(melding -> {
            Veileder veileder = veilederService.hentVeileder(melding.getOpprettetAvIdent());
            melding.setOpprettetAvNavn(veileder.getNavn());
        });

        return meldinger;
    }

}
