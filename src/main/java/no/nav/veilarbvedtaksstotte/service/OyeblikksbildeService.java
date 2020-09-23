package no.nav.veilarbvedtaksstotte.service;

import no.nav.veilarbvedtaksstotte.client.api.EgenvurderingClient;
import no.nav.veilarbvedtaksstotte.client.api.RegistreringClient;
import no.nav.veilarbvedtaksstotte.client.api.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.domain.Oyeblikksbilde;
import no.nav.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.OyeblikksbildeRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

import static no.nav.veilarbvedtaksstotte.domain.enums.OyeblikksbildeType.*;


@Service
public class OyeblikksbildeService {

    private final AuthService authService;
    private final OyeblikksbildeRepository oyeblikksbildeRepository;
    private final VedtaksstotteRepository vedtaksstotteRepository;
    private final VeilarbpersonClient veilarbpersonClient;
    private final RegistreringClient registreringClient;
    private final EgenvurderingClient egenvurderingClient;

    @Autowired
    public OyeblikksbildeService(
            AuthService authService,
            OyeblikksbildeRepository oyeblikksbildeRepository,
            VedtaksstotteRepository vedtaksstotteRepository,
            VeilarbpersonClient veilarbpersonClient,
            RegistreringClient registreringClient,
            EgenvurderingClient egenvurderingClient
    ) {
        this.oyeblikksbildeRepository = oyeblikksbildeRepository;
        this.authService = authService;
        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.veilarbpersonClient = veilarbpersonClient;
        this.registreringClient = registreringClient;
        this.egenvurderingClient = egenvurderingClient;
    }

    public List<Oyeblikksbilde> hentOyeblikksbildeForVedtak(long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);
        authService.sjekkTilgangTilAktorId(vedtak.getAktorId());
        return oyeblikksbildeRepository.hentOyeblikksbildeForVedtak(vedtakId);
    }

    void lagreOyeblikksbilde(String fnr, long vedtakId) {
        final String cvOgJobbprofilData = veilarbpersonClient.hentCVOgJobbprofil(fnr);
        final String registreringData = registreringClient.hentRegistreringDataJson(fnr);
        final String egenvurderingData = egenvurderingClient.hentEgenvurdering(fnr);

        List<Oyeblikksbilde> oyeblikksbilde = Arrays.asList(
                new Oyeblikksbilde(vedtakId, CV_OG_JOBBPROFIL, cvOgJobbprofilData),
                new Oyeblikksbilde(vedtakId, REGISTRERINGSINFO, registreringData),
                new Oyeblikksbilde(vedtakId, EGENVURDERING, egenvurderingData)
        );

        oyeblikksbildeRepository.lagOyeblikksbilde(oyeblikksbilde);
    }
}
