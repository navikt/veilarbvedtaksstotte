package no.nav.veilarbvedtaksstotte.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.poao_tilgang.client.TilgangType;
import no.nav.veilarbvedtaksstotte.client.aiaBackend.AiaBackendClient;
import no.nav.veilarbvedtaksstotte.client.aiaBackend.dto.EgenvurderingResponseDTO;
import no.nav.veilarbvedtaksstotte.client.aiaBackend.request.EgenvurderingForPersonRequest;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret.ArbeidssoekerRegisteretService;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret.OpplysningerOmArbeidssoekerMedProfilering;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.person.dto.CvDto;
import no.nav.veilarbvedtaksstotte.domain.VedtakOpplysningKilder;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.*;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.OyeblikksbildeRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.veilarbvedtaksstotte.utils.SecureLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
@Slf4j
public class OyeblikksbildeService {

    private final AuthService authService;
    private final OyeblikksbildeRepository oyeblikksbildeRepository;
    private final VedtaksstotteRepository vedtaksstotteRepository;
    private final VeilarbpersonClient veilarbpersonClient;
    private final AiaBackendClient aiaBackendClient;
    private final ArbeidssoekerRegisteretService arbeidssoekerRegisteretService;

    @Autowired
    public OyeblikksbildeService(
            AuthService authService,
            OyeblikksbildeRepository oyeblikksbildeRepository,
            VedtaksstotteRepository vedtaksstotteRepository,
            VeilarbpersonClient veilarbpersonClient,
            AiaBackendClient aiaBackendClient,
            ArbeidssoekerRegisteretService arbeidssoekerRegisteretService
    ) {
        this.oyeblikksbildeRepository = oyeblikksbildeRepository;
        this.authService = authService;
        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.veilarbpersonClient = veilarbpersonClient;
        this.aiaBackendClient = aiaBackendClient;
        this.arbeidssoekerRegisteretService = arbeidssoekerRegisteretService;
    }

    // Kun brukt i test
    public List<OyeblikksbildeDto> hentOyeblikksbildeForVedtak(long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);
        authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, AktorId.of(vedtak.getAktorId()));
        return oyeblikksbildeRepository.hentOyeblikksbildeForVedtak(vedtakId);
    }

    public OyeblikksbildeCvDto hentCVOyeblikksbildeForVedtak(long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);
        authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, AktorId.of(vedtak.getAktorId()));

        Optional<OyeblikksbildeCvDto> oyeblikksbildeCvDto = oyeblikksbildeRepository.hentCVOyeblikksbildeForVedtak(vedtakId);

        return oyeblikksbildeCvDto.orElseGet(() -> new OyeblikksbildeCvDto(null, false));
    }

    public OyeblikksbildeArbeidssokerRegistretDto hentArbeidssokerRegistretOyeblikksbildeForVedtak(long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);
        authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, AktorId.of(vedtak.getAktorId()));
        Optional<OyeblikksbildeArbeidssokerRegistretDto> oyeblikksbildeRegistreringDto = oyeblikksbildeRepository.hentArbeidssokerRegistretOyeblikksbildeForVedtak(vedtakId);

        return oyeblikksbildeRegistreringDto.orElseGet(() -> new OyeblikksbildeArbeidssokerRegistretDto(null, false));
    }

    public OyeblikksbildeEgenvurderingDto hentEgenvurderingOyeblikksbildeForVedtak(long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);
        authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, AktorId.of(vedtak.getAktorId()));
        Optional<OyeblikksbildeEgenvurderingDto> oyeblikksbildeEgenvurderingDto = oyeblikksbildeRepository.hentEgenvurderingOyeblikksbildeForVedtak(vedtakId);

        return oyeblikksbildeEgenvurderingDto.orElseGet(() -> new OyeblikksbildeEgenvurderingDto(null, false));
    }

    public OyeblikksbildeRegistreringDto hentRegistreringOyeblikksbildeForVedtak(long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);
        authService.sjekkTilgangTilBrukerOgEnhet(TilgangType.SKRIVE, AktorId.of(vedtak.getAktorId()));
        Optional<OyeblikksbildeRegistreringDto> oyeblikksbildeRegistreringDto = oyeblikksbildeRepository.hentRegistreringOyeblikksbildeForVedtak(vedtakId);
        return oyeblikksbildeRegistreringDto.orElseGet(() -> new OyeblikksbildeRegistreringDto(null, false));
    }

    public List<OyeblikksbildeDto> hentOyeblikksbildeForVedtakJournalforing(long vedtakId) {
        return oyeblikksbildeRepository.hentOyeblikksbildeForVedtak(vedtakId);
    }

    public void lagreJournalfortDokumentId(long vedtakId, String dokumentId, OyeblikksbildeType oyeblikksbildeType) {
        oyeblikksbildeRepository.lagreJournalfortDokumentId(vedtakId, dokumentId, oyeblikksbildeType);
    }

    public String hentJournalfortDokumentId(long vedtakId, OyeblikksbildeType oyeblikksbildeType) {
        return oyeblikksbildeRepository.hentJournalfortDokumentId(vedtakId, oyeblikksbildeType);
    }

    public void slettOyeblikksbilde(long vedtakId) {
        oyeblikksbildeRepository.slettOyeblikksbilder(vedtakId);
    }

    void lagreOyeblikksbilde(String fnr, long vedtakId, List<String> kilder) {

        if (kilder == null || kilder.isEmpty()) {
            SecureLog.getSecureLog().warn(String.format("Ingen kilder valgt for vedtak med id: %s", vedtakId));
            return;
        }

        if (kilder.stream().anyMatch(kilde -> kilde.contains(VedtakOpplysningKilder.CV.getDesc()) || kilde.contains(VedtakOpplysningKilder.CV_NN.getDesc()))) {
            final CvDto cvOgJobbprofilData = veilarbpersonClient.hentCVOgJobbprofil(fnr);
            oyeblikksbildeRepository.upsertCVOyeblikksbilde(vedtakId, cvOgJobbprofilData);
        }
        if (kilder.stream().anyMatch(kilde -> kilde.contains(VedtakOpplysningKilder.REGISTRERING.getDesc()) || kilde.contains(VedtakOpplysningKilder.ARBEIDSSOKERREGISTERET.getDesc()) || kilde.contains(VedtakOpplysningKilder.ARBEIDSSOKERREGISTERET_NN.getDesc()))) {
            OpplysningerOmArbeidssoekerMedProfilering opplysningerOmArbeidssoekerMedProfilering = arbeidssoekerRegisteretService.hentSisteOpplysningerOmArbeidssoekerMedProfilering(Fnr.of(fnr));
            oyeblikksbildeRepository.upsertArbeidssokerRegistretOyeblikksbilde(vedtakId, opplysningerOmArbeidssoekerMedProfilering);
        }
        if (kilder.stream().anyMatch(kilde -> kilde.contains(VedtakOpplysningKilder.EGENVURDERING.getDesc()) || kilde.contains(VedtakOpplysningKilder.EGENVURDERING_NN.getDesc()))) {
            final EgenvurderingResponseDTO egenvurdering = aiaBackendClient.hentEgenvurdering(new EgenvurderingForPersonRequest(fnr));
            EgenvurderingDto egenvurderingData = mapToEgenvurderingData(egenvurdering);
            oyeblikksbildeRepository.upsertEgenvurderingOyeblikksbilde(vedtakId, egenvurderingData);
        }
    }

    public EgenvurderingDto mapToEgenvurderingData(EgenvurderingResponseDTO egenvurderingResponseDTO) {//public for test
        List<EgenvurderingDto.Svar> svar = new ArrayList<>();
        if (egenvurderingResponseDTO != null) {
            String svartekst = egenvurderingResponseDTO.getTekster().getSvar().get(egenvurderingResponseDTO.getOppfolging());
            svar.add(new EgenvurderingDto.Svar(
                    egenvurderingResponseDTO.getTekster().getSporsmal(),
                    svartekst,
                    egenvurderingResponseDTO.getOppfolging(),
                    egenvurderingResponseDTO.getDialogId()
            ));
            return new EgenvurderingDto(egenvurderingResponseDTO.getDato(), svar);
        }
        return null;
    }

}
