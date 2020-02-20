package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.fo.veilarbvedtaksstotte.client.CVClient;
import no.nav.fo.veilarbvedtaksstotte.client.EgenvurderingClient;
import no.nav.fo.veilarbvedtaksstotte.client.RegistreringClient;
import no.nav.fo.veilarbvedtaksstotte.domain.Oyeblikksbilde;
import no.nav.fo.veilarbvedtaksstotte.repository.OyeblikksbildeRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

import static no.nav.fo.veilarbvedtaksstotte.domain.enums.OyeblikksbildeType.*;


@Service
public class OyeblikksbildeService {

    private final AuthService authService;
    private final OyeblikksbildeRepository oyeblikksbildeRepository;
    private CVClient cvClient;
    private RegistreringClient registreringClient;
    private EgenvurderingClient egenvurderingClient;

    public OyeblikksbildeService(AuthService authService,
                                 OyeblikksbildeRepository oyeblikksbildeRepository,
                                 CVClient cvClient,
                                 RegistreringClient registreringClient,
                                 EgenvurderingClient egenvurderingClient) {
        this.oyeblikksbildeRepository = oyeblikksbildeRepository;
        this.authService = authService;
        this.cvClient = cvClient;
        this.registreringClient = registreringClient;
        this.egenvurderingClient = egenvurderingClient;
    }

    public List<Oyeblikksbilde> hentOyeblikksbildeForVedtak(String fnr, long vedtakId) {
        authService.sjekkTilgang(fnr);
        return oyeblikksbildeRepository.hentOyeblikksbildeForVedtak(vedtakId);
    }

    void lagreOyeblikksbilde(String fnr, long vedtakId) {
        final String cvData = cvClient.hentCV(fnr);
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
