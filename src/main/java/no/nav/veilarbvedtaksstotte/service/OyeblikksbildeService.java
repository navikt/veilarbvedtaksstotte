package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.types.identer.AktorId;

import no.nav.veilarbvedtaksstotte.client.egenvurdering.EgenvurderingClient;
import no.nav.veilarbvedtaksstotte.client.egenvurdering.EgenvurderingData;
import no.nav.veilarbvedtaksstotte.client.egenvurdering.EgenvurderingForPersonDTO;
import no.nav.veilarbvedtaksstotte.client.egenvurdering.EgenvurderingResponseDTO;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClient;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.Oyeblikksbilde;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.OyeblikksbildeRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.veilarbvedtaksstotte.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static no.nav.common.json.JsonUtils.toJson;
import static no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeType.*;


@Service
public class OyeblikksbildeService {

    private final AuthService authService;
    private final OyeblikksbildeRepository oyeblikksbildeRepository;
    private final VedtaksstotteRepository vedtaksstotteRepository;
    private final VeilarbpersonClient veilarbpersonClient;
    private final VeilarbregistreringClient registreringClient;
    private final EgenvurderingClient egenvurderingClient;

    @Autowired
    public OyeblikksbildeService(
            AuthService authService,
            OyeblikksbildeRepository oyeblikksbildeRepository,
            VedtaksstotteRepository vedtaksstotteRepository,
            VeilarbpersonClient veilarbpersonClient,
            VeilarbregistreringClient registreringClient,
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
        authService.sjekkTilgangTilBrukerOgEnhet(AktorId.of(vedtak.getAktorId()));
        return oyeblikksbildeRepository.hentOyeblikksbildeForVedtak(vedtakId);
    }

    public void slettOyeblikksbilde(long vedtakId) {
        oyeblikksbildeRepository.slettOyeblikksbilder(vedtakId);
    }

    void lagreOyeblikksbilde(String fnr, long vedtakId) {
        final String cvOgJobbprofilData = veilarbpersonClient.hentCVOgJobbprofil(fnr);
        final String registreringData = registreringClient.hentRegistreringDataJson(fnr);
        final EgenvurderingResponseDTO egenvurdering = egenvurderingClient.hentEgenvurdering(new EgenvurderingForPersonDTO(fnr));
        final String egenvurderingData = mapEgenvurderingTilGammelStruktur(egenvurdering);

        List<Oyeblikksbilde> oyeblikksbilde = Arrays.asList(
                new Oyeblikksbilde(vedtakId, CV_OG_JOBBPROFIL, cvOgJobbprofilData),
                new Oyeblikksbilde(vedtakId, REGISTRERINGSINFO, registreringData),
                new Oyeblikksbilde(vedtakId, EGENVURDERING, egenvurderingData)
        );

        oyeblikksbilde.forEach(oyeblikksbildeRepository::upsertOyeblikksbilde);
    }

  public String mapEgenvurderingTilGammelStruktur(EgenvurderingResponseDTO egenvurderingResponseDTO) {//public for test
        if(egenvurderingResponseDTO == null) {
            return JsonUtils.createNoDataStr("Bruker har ikke fylt ut egenvurdering");
        }
        List<EgenvurderingData.Svar> svar = new ArrayList<>();
        String svartekst = egenvurderingResponseDTO.getTekster().getSvar().get(egenvurderingResponseDTO.getOppfolging());
        svar.add(new EgenvurderingData.Svar(
            egenvurderingResponseDTO.getTekster().getSporsmal(),
            svartekst,
            egenvurderingResponseDTO.getOppfolging(),
            egenvurderingResponseDTO.getDialogId()
        ));

        EgenvurderingData egenvurderingData = new EgenvurderingData(egenvurderingResponseDTO.getDato(), svar);
        return toJson(egenvurderingData);
    }
}
