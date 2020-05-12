package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.client.EgenvurderingClient;
import no.nav.veilarbvedtaksstotte.client.PamCvClient;
import no.nav.veilarbvedtaksstotte.client.RegistreringClient;
import no.nav.veilarbvedtaksstotte.domain.Oyeblikksbilde;
import no.nav.veilarbvedtaksstotte.repository.OyeblikksbildeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

import static no.nav.veilarbvedtaksstotte.domain.enums.OyeblikksbildeType.*;


@Service
public class OyeblikksbildeService {

    private final AuthService authService;
    private final OyeblikksbildeRepository oyeblikksbildeRepository;
    private PamCvClient pamCvClient;
    private RegistreringClient registreringClient;
    private EgenvurderingClient egenvurderingClient;

    @Autowired
    public OyeblikksbildeService(AuthService authService,
                                 OyeblikksbildeRepository oyeblikksbildeRepository,
                                 PamCvClient pamCvClient,
                                 RegistreringClient registreringClient,
                                 EgenvurderingClient egenvurderingClient) {
        this.oyeblikksbildeRepository = oyeblikksbildeRepository;
        this.authService = authService;
        this.pamCvClient = pamCvClient;
        this.registreringClient = registreringClient;
        this.egenvurderingClient = egenvurderingClient;
    }

    public List<Oyeblikksbilde> hentOyeblikksbildeForVedtak(String fnr, long vedtakId) {
        authService.sjekkTilgang(fnr);
        return oyeblikksbildeRepository.hentOyeblikksbildeForVedtak(vedtakId);
    }

    void lagreOyeblikksbilde(String fnr, long vedtakId) {
        final String cvData = pamCvClient.hentCV(fnr);
        final String registreringData = registreringClient.hentRegistreringDataJson(fnr);
        final String egenvurderingData = egenvurderingClient.hentEgenvurdering(fnr);

        List<Oyeblikksbilde> oyeblikksbilde = Arrays.asList(
                new Oyeblikksbilde(vedtakId, CV_OG_JOBBPROFIL, cvData),
                new Oyeblikksbilde(vedtakId, REGISTRERINGSINFO, registreringData),
                new Oyeblikksbilde(vedtakId, EGENVURDERING, egenvurderingData)
        );

        oyeblikksbildeRepository.lagOyeblikksbilde(oyeblikksbilde);
    }
}
