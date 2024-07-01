package no.nav.veilarbvedtaksstotte.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.aiaBackend.AiaBackendClient;
import no.nav.veilarbvedtaksstotte.client.aiaBackend.dto.BesvarelseSvar;
import no.nav.veilarbvedtaksstotte.client.aiaBackend.dto.EgenvurderingResponseDTO;
import no.nav.veilarbvedtaksstotte.client.aiaBackend.dto.EndringIRegistreringsdataResponse;
import no.nav.veilarbvedtaksstotte.client.aiaBackend.request.EgenvurderingForPersonRequest;
import no.nav.veilarbvedtaksstotte.client.aiaBackend.request.EndringIRegistreringdataRequest;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret.ArbeidssoekerRegisteretService;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret.MetadataResponse;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret.OpplysningerOmArbeidssoekerMedProfilering;
import no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret.OpplysningerOmArbeidssoekerResponse;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.person.dto.CvDto;
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClient;
import no.nav.veilarbvedtaksstotte.client.registrering.dto.BrukerRegistreringType;
import no.nav.veilarbvedtaksstotte.client.registrering.dto.RegistreringResponseDto;
import no.nav.veilarbvedtaksstotte.client.registrering.dto.RegistreringsdataDto;
import no.nav.veilarbvedtaksstotte.domain.VedtakOpplysningKilder;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.*;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.OyeblikksbildeRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.veilarbvedtaksstotte.utils.SecureLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
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
    private final VeilarbregistreringClient registreringClient;
    private final AiaBackendClient aiaBackendClient;
    private final ArbeidssoekerRegisteretService arbeidssoekerRegisteretService;

    @Autowired
    public OyeblikksbildeService(
            AuthService authService,
            OyeblikksbildeRepository oyeblikksbildeRepository,
            VedtaksstotteRepository vedtaksstotteRepository,
            VeilarbpersonClient veilarbpersonClient,
            VeilarbregistreringClient registreringClient,
            AiaBackendClient aiaBackendClient,
            ArbeidssoekerRegisteretService arbeidssoekerRegisteretService
    ) {
        this.oyeblikksbildeRepository = oyeblikksbildeRepository;
        this.authService = authService;
        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.veilarbpersonClient = veilarbpersonClient;
        this.registreringClient = registreringClient;
        this.aiaBackendClient = aiaBackendClient;
        this.arbeidssoekerRegisteretService = arbeidssoekerRegisteretService;
    }

    public List<OyeblikksbildeDto> hentOyeblikksbildeForVedtak(long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);
        authService.sjekkTilgangTilBrukerOgEnhet(AktorId.of(vedtak.getAktorId()));
        return oyeblikksbildeRepository.hentOyeblikksbildeForVedtak(vedtakId);
    }

    public OyeblikksbildeCvDto hentCVOyeblikksbildeForVedtak(long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);
        authService.sjekkTilgangTilBrukerOgEnhet(AktorId.of(vedtak.getAktorId()));

        Optional<OyeblikksbildeCvDto> oyeblikksbildeCvDto = oyeblikksbildeRepository.hentCVOyeblikksbildeForVedtak(vedtakId);

        return oyeblikksbildeCvDto.orElseGet(() -> new OyeblikksbildeCvDto(null, false));
    }

    public OyeblikksbildeRegistreringDto hentRegistreringOyeblikksbildeForVedtak(long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);
        authService.sjekkTilgangTilBrukerOgEnhet(AktorId.of(vedtak.getAktorId()));
        Optional<OyeblikksbildeRegistreringDto> oyeblikksbildeRegistreringDto = oyeblikksbildeRepository.hentRegistreringOyeblikksbildeForVedtak(vedtakId);

        return oyeblikksbildeRegistreringDto.orElseGet(() -> new OyeblikksbildeRegistreringDto(null, false));
    }

    public OyeblikksbildeArbeidssokerRegistretDto hentArbeidssokerRegistretOyeblikksbildeForVedtak(long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);
        authService.sjekkTilgangTilBrukerOgEnhet(AktorId.of(vedtak.getAktorId()));
        Optional<OyeblikksbildeArbeidssokerRegistretDto> oyeblikksbildeRegistreringDto = oyeblikksbildeRepository.hentArbeidssokerRegistretOyeblikksbildeForVedtak(vedtakId);

        return oyeblikksbildeRegistreringDto.orElseGet(() -> new OyeblikksbildeArbeidssokerRegistretDto(null, false));
    }

    public OyeblikksbildeEgenvurderingDto hentEgenvurderingOyeblikksbildeForVedtak(long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);
        authService.sjekkTilgangTilBrukerOgEnhet(AktorId.of(vedtak.getAktorId()));
        Optional<OyeblikksbildeEgenvurderingDto> oyeblikksbildeEgenvurderingDto = oyeblikksbildeRepository.hentEgenvurderingOyeblikksbildeForVedtak(vedtakId);

        return oyeblikksbildeEgenvurderingDto.orElseGet(() -> new OyeblikksbildeEgenvurderingDto(null, false));
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

        if (kilder.stream().anyMatch(kilde -> kilde.equals(VedtakOpplysningKilder.CV.getDesc()))) {
            final CvDto cvOgJobbprofilData = veilarbpersonClient.hentCVOgJobbprofil(fnr);
            oyeblikksbildeRepository.upsertCVOyeblikksbilde(vedtakId, cvOgJobbprofilData);
        }
        if (kilder.stream().anyMatch(kilde -> kilde.equals(VedtakOpplysningKilder.REGISTRERING.getDesc()))) {
            lagreRegistreringsData(fnr, vedtakId);
        }
        if (kilder.stream().anyMatch(kilde -> kilde.equals(VedtakOpplysningKilder.EGENVURDERING.getDesc()))) {
            final EgenvurderingResponseDTO egenvurdering = aiaBackendClient.hentEgenvurdering(new EgenvurderingForPersonRequest(fnr));
            EgenvurderingDto egenvurderingData = mapToEgenvurderingData(egenvurdering);
            oyeblikksbildeRepository.upsertEgenvurderingOyeblikksbilde(vedtakId, egenvurderingData);
        }
    }

    private void lagreRegistreringsData(String fnr, long vedtakId){
        OpplysningerOmArbeidssoekerMedProfilering opplysningerOmArbeidssoekerMedProfilering = arbeidssoekerRegisteretService.hentSisteOpplysningerOmArbeidssoekerMedProfilering(Fnr.of(fnr));

        final RegistreringResponseDto registreringData = registreringClient.hentRegistreringData(fnr);
        final EndringIRegistreringsdataResponse endringIRegistreringsdata = aiaBackendClient.hentEndringIRegistreringdata(new EndringIRegistreringdataRequest(fnr));
        final RegistreringResponseDto oppdaterteRegistreringsData = oppdaterRegistreringsdataHvisNyeEndringer(registreringData, endringIRegistreringsdata);

        if (oppdaterteRegistreringsData != null && erSykemeldt(opplysningerOmArbeidssoekerMedProfilering, oppdaterteRegistreringsData)){
            oyeblikksbildeRepository.upsertRegistreringOyeblikksbilde(vedtakId, oppdaterteRegistreringsData);
        }else{
            oyeblikksbildeRepository.upsertArbeidssokerRegistretOyeblikksbilde(vedtakId, opplysningerOmArbeidssoekerMedProfilering);
        }
    }

    private boolean erSykemeldt(OpplysningerOmArbeidssoekerMedProfilering opplysningerOmArbeidssoekerMedProfilering, RegistreringResponseDto registreringData){
        if (opplysningerOmArbeidssoekerMedProfilering == null && registreringData.getType().equals(BrukerRegistreringType.SYKMELDT)){
            return true;
        }else if (opplysningerOmArbeidssoekerMedProfilering != null && registreringData.getType().equals(BrukerRegistreringType.SYKMELDT)){
            return erSykemeldtEtterArbeidssokerRegistrering(opplysningerOmArbeidssoekerMedProfilering.getOpplysningerOmArbeidssoeker(), registreringData.getRegistrering());
        }
        return false;
    }

    private boolean erSykemeldtEtterArbeidssokerRegistrering(OpplysningerOmArbeidssoekerResponse opplysningerOmArbeidssoeker, RegistreringsdataDto registreringData){
        Optional<LocalDateTime> opplysningerOmArbeidssoekerOprettetDato = Optional.ofNullable(opplysningerOmArbeidssoeker)
                .map(OpplysningerOmArbeidssoekerResponse::getSendtInnAv)
                .map(MetadataResponse::getTidspunkt)
                .map(ZonedDateTime::toLocalDateTime);
        Optional<LocalDateTime> registreringEndretDato = Optional.ofNullable(registreringData).map(RegistreringsdataDto::getEndretTidspunkt);

        if (opplysningerOmArbeidssoekerOprettetDato.isPresent() && registreringEndretDato.isPresent()){
            return registreringEndretDato.get().isAfter(opplysningerOmArbeidssoekerOprettetDato.get());
        }

        return false;
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

    public RegistreringResponseDto oppdaterRegistreringsdataHvisNyeEndringer(RegistreringResponseDto registreringsData, EndringIRegistreringsdataResponse endringIRegistreringsdata) { //public for test
        if (registreringsData == null || endringIRegistreringsdata == null || endringIRegistreringsdata.getErBesvarelsenEndret() == null || !endringIRegistreringsdata.getErBesvarelsenEndret()) {
            return registreringsData;
        }

        try {
            if (endringIRegistreringsdata.getEndretAv() != null) {
                registreringsData.getRegistrering().setEndretAv(endringIRegistreringsdata.getEndretAv());
            }

            if (endringIRegistreringsdata.getEndretTidspunkt() != null) {
                registreringsData.getRegistrering().setEndretTidspunkt(endringIRegistreringsdata.getEndretTidspunkt());
            }

            if (endringIRegistreringsdata.getBesvarelse() != null && endringIRegistreringsdata.getBesvarelse().getDinSituasjon() != null && registreringsData.getRegistrering().getBesvarelse() != null) {
                BesvarelseSvar besvarelse = registreringsData.getRegistrering().getBesvarelse();
                besvarelse.setDinSituasjon(BesvarelseSvar.DinSituasjonSvar.valueOf(endringIRegistreringsdata.getBesvarelse().getDinSituasjon().getVerdi()));
                registreringsData.getRegistrering().setBesvarelse(besvarelse);
            }


            if (endringIRegistreringsdata.getBesvarelse() != null && endringIRegistreringsdata.getBesvarelse().getDinSituasjon() != null && endringIRegistreringsdata.getBesvarelse().getDinSituasjon().getVerdi() != null) {
                List<RegistreringsdataDto.TekstForSporsmal> teksterForBesvarelse = registreringsData.getRegistrering().getTeksterForBesvarelse();
                teksterForBesvarelse.stream().filter(t -> t.getSporsmalId().equals("dinSituasjon")).forEach(t ->
                        t.setSvar(mapDinSituasjonVerdiToTekst(endringIRegistreringsdata.getBesvarelse().getDinSituasjon().getVerdi()))
                );
                registreringsData.getRegistrering().setTeksterForBesvarelse(teksterForBesvarelse);
            }

            return registreringsData;
        } catch (Exception e) {
            log.error("Kunne ikke oppdatere registrerings data " + e, e);
            throw e;
        }
    }

    public String mapDinSituasjonVerdiToTekst(String dinSituasjonVerdi) {
        return switch (dinSituasjonVerdi) {
            case "MISTET_JOBBEN" -> "Har mistet eller kommer til å miste jobben";
            case "HAR_SAGT_OPP" -> "Har sagt opp eller kommer til å si opp";
            case "DELTIDSJOBB_VIL_MER" -> "Har deltidsjobb, men vil jobbe mer";
            case "ALDRI_HATT_JOBB" -> "Har aldri vært i jobb";
            case "VIL_BYTTE_JOBB" -> "Har jobb, men vil bytte";
            case "JOBB_OVER_2_AAR" -> "Har ikke vært i jobb de siste 2 årene";
            case "ER_PERMITTERT" -> "Er permittert eller kommer til å bli permittert";
            case "USIKKER_JOBBSITUASJON" -> "Er usikker på jobbsituasjonen min";
            case "AKKURAT_FULLFORT_UTDANNING" -> "Har akkurat fullført utdanning; militærtjeneste eller annet";
            case "VIL_FORTSETTE_I_JOBB" -> "Har jobb og ønsker å fortsette i den jobben jeg er i";
            case "OPPSIGELSE" -> "Jeg har blitt sagt opp av arbeidsgiver";
            case "ENDRET_PERMITTERINGSPROSENT" -> "Arbeidsgiver har endret permitteringen min";
            case "TILBAKE_TIL_JOBB" -> "Jeg skal tilbake i jobb hos min nåværende arbeidsgiver";
            case "NY_JOBB" -> "Jeg skal begynne å jobbe hos en annen arbeidsgiver";
            case "MIDLERTIDIG_JOBB" -> "Jeg har fått midlertidig jobb hos en annen arbeidsgiver";
            case "KONKURS" -> "Arbeidsgiveren min er konkurs";
            case "SAGT_OPP" -> "Jeg har sagt opp jobben min";
            case "ANNET" -> "Situasjonen min har endret seg, men ingen av valgene passet";
            default -> "";
        };
    }

}
