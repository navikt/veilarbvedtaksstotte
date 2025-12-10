package no.nav.veilarbvedtaksstotte.service;

import io.getunleash.DefaultUnleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NorskIdent;
import no.nav.poao_tilgang.client.TilgangType;
import no.nav.veilarbvedtaksstotte.client.aiaBackend.AiaBackendClient;
import no.nav.veilarbvedtaksstotte.client.aiaBackend.dto.EgenvurderingResponseDTO;
import no.nav.veilarbvedtaksstotte.client.aiaBackend.request.EgenvurderingForPersonRequest;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.ArbeidssoekerregisteretApiOppslagV2Client;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.ArbeidssoekerregisteretApiOppslagV2ClientImpl;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekerregisteret.EgenvurderingDialogTjenesteClient;
import no.nav.veilarbvedtaksstotte.client.person.OpplysningerOmArbeidssoekerMedProfilering;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.person.dto.CvDto;
import no.nav.veilarbvedtaksstotte.domain.VedtakOpplysningKilder;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.EgenvurderingDto;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.EgenvurderingV2Dto;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeArbeidssokerRegistretDto;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeCvDto;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeDto;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeEgenvurderingDto;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeRegistreringDto;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeType;
import no.nav.veilarbvedtaksstotte.domain.vedtak.KildeEntity;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.OyeblikksbildeRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static no.nav.veilarbvedtaksstotte.utils.SecureLog.secureLog;
import static no.nav.veilarbvedtaksstotte.utils.UnleashUtilsKt.BRUK_NY_KILDE_FOR_EGENVURDERING;


@Service
@Slf4j
public class OyeblikksbildeService {

    private final AuthService authService;
    private final OyeblikksbildeRepository oyeblikksbildeRepository;
    private final VedtaksstotteRepository vedtaksstotteRepository;
    private final VeilarbpersonClient veilarbpersonClient;
    private final AiaBackendClient aiaBackendClient;
    private final ArbeidssoekerregisteretApiOppslagV2Client arbeidssoekerregisteretApiOppslagV2Client;
    private final EgenvurderingDialogTjenesteClient egenvurderingDialogTjenesteClient;
    private final DefaultUnleash defaultUnleash;

    @Autowired
    public OyeblikksbildeService(
            AuthService authService,
            OyeblikksbildeRepository oyeblikksbildeRepository,
            VedtaksstotteRepository vedtaksstotteRepository,
            VeilarbpersonClient veilarbpersonClient,
            AiaBackendClient aiaBackendClient,
            ArbeidssoekerregisteretApiOppslagV2Client arbeidssoekerregisteretApiOppslagV2Client,
            EgenvurderingDialogTjenesteClient egenvurderingDialogTjenesteClient,
            DefaultUnleash defaultUnleash
    ) {
        this.oyeblikksbildeRepository = oyeblikksbildeRepository;
        this.authService = authService;
        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.veilarbpersonClient = veilarbpersonClient;
        this.aiaBackendClient = aiaBackendClient;
        this.defaultUnleash = defaultUnleash;
        this.arbeidssoekerregisteretApiOppslagV2Client = arbeidssoekerregisteretApiOppslagV2Client;
        this.egenvurderingDialogTjenesteClient = egenvurderingDialogTjenesteClient;
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

    void lagreOyeblikksbilde(String fnr, long vedtakId, List<KildeEntity> kilder) {
        if (kilder == null || kilder.isEmpty()) {
            secureLog.warn("Ingen kilder valgt for vedtak med id: {}", vedtakId);
            return;
        }

        List<String> kildeTekster = kilder.stream().map(KildeEntity::getTekst).toList();

        if (kilder.stream().anyMatch(kilde -> kildeTekster.contains(VedtakOpplysningKilder.CV.getDesc()) || kildeTekster.contains(VedtakOpplysningKilder.CV_NN.getDesc()))) {
            final CvDto cvOgJobbprofilData = veilarbpersonClient.hentCVOgJobbprofil(fnr);
            oyeblikksbildeRepository.upsertCVOyeblikksbilde(vedtakId, cvOgJobbprofilData);
        }
        if (kilder.stream().anyMatch(kilde ->
                kildeTekster.contains(VedtakOpplysningKilder.REGISTRERING.getDesc())
                        || kildeTekster.contains(VedtakOpplysningKilder.ARBEIDSSOKERREGISTERET.getDesc())
                        || kildeTekster.contains(VedtakOpplysningKilder.ARBEIDSSOKERREGISTERET_NN.getDesc()))
        ) {
            OpplysningerOmArbeidssoekerMedProfilering opplysningerOmArbeidssoekerMedProfilering = veilarbpersonClient.hentSisteOpplysningerOmArbeidssoekerMedProfilering(Fnr.of(fnr));
            oyeblikksbildeRepository.upsertArbeidssokerRegistretOyeblikksbilde(vedtakId, opplysningerOmArbeidssoekerMedProfilering);
        }
        if (kilder.stream().anyMatch(kilde -> kildeTekster.contains(VedtakOpplysningKilder.EGENVURDERING.getDesc()) || kildeTekster.contains(VedtakOpplysningKilder.EGENVURDERING_NN.getDesc()))) {

            if (defaultUnleash.isEnabled(BRUK_NY_KILDE_FOR_EGENVURDERING)) {
                // Dersom toggle på bruk paw-arbeidssoekerregisteret-api-oppslag-v2
                EgenvurderingV2Dto egenvurderingV2Dto = Optional.ofNullable(arbeidssoekerregisteretApiOppslagV2Client.hentEgenvurdering(NorskIdent.of(fnr)))
                        .map(aggregertPeriode -> {
                            Long dialogId = egenvurderingDialogTjenesteClient.hentDialogId(aggregertPeriode.getId()).getDialogId();
                            return ArbeidssoekerregisteretApiOppslagV2ClientImpl.mapToEgenvurderingV2Dto(aggregertPeriode, dialogId);
                        })
                        .orElse(null);
                oyeblikksbildeRepository.upsertEgenvurderingV2Oyeblikksbilde(vedtakId, egenvurderingV2Dto);
            } else {
                // Dersom toggle av bruk aia-backend
                EgenvurderingDto egenvurderingDto = Optional.ofNullable(aiaBackendClient.hentEgenvurdering(new EgenvurderingForPersonRequest(fnr)))
                        .map(OyeblikksbildeService::mapToEgenvurderingData)
                        .orElse(null);
                oyeblikksbildeRepository.upsertEgenvurderingOyeblikksbilde(vedtakId, egenvurderingDto);
            }

        }
    }

    public static EgenvurderingDto mapToEgenvurderingData(EgenvurderingResponseDTO egenvurderingResponseDTO) {//public for test
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
