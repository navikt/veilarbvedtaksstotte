package no.nav.veilarbvedtaksstotte.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarbvedtaksstotte.client.aiaBackend.*;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.person.dto.CvDto;
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClient;
import no.nav.veilarbvedtaksstotte.client.registrering.dto.RegistreringResponseDto;
import no.nav.veilarbvedtaksstotte.client.registrering.dto.RegistreringsdataDto;
import no.nav.veilarbvedtaksstotte.domain.VedtakOpplysningKilder;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeDto;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeEgenvurderingDto;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeType;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.OyeblikksbildeRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.veilarbvedtaksstotte.utils.SecureLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@Slf4j
public class OyeblikksbildeService {

    private final AuthService authService;
    private final OyeblikksbildeRepository oyeblikksbildeRepository;
    private final VedtaksstotteRepository vedtaksstotteRepository;
    private final VeilarbpersonClient veilarbpersonClient;
    private final VeilarbregistreringClient registreringClient;
    private final AiaBackendClient aiaBackendClient;

    @Autowired
    public OyeblikksbildeService(
            AuthService authService,
            OyeblikksbildeRepository oyeblikksbildeRepository,
            VedtaksstotteRepository vedtaksstotteRepository,
            VeilarbpersonClient veilarbpersonClient,
            VeilarbregistreringClient registreringClient,
            AiaBackendClient aiaBackendClient
    ) {
        this.oyeblikksbildeRepository = oyeblikksbildeRepository;
        this.authService = authService;
        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.veilarbpersonClient = veilarbpersonClient;
        this.registreringClient = registreringClient;
        this.aiaBackendClient = aiaBackendClient;
    }

    public List<OyeblikksbildeDto> hentOyeblikksbildeForVedtak(long vedtakId) {
        Vedtak vedtak = vedtaksstotteRepository.hentVedtak(vedtakId);
        authService.sjekkTilgangTilBrukerOgEnhet(AktorId.of(vedtak.getAktorId()));
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
            final RegistreringResponseDto registreringData = registreringClient.hentRegistreringData(fnr);
            final EndringIRegistreringsdataResponse endringIRegistreringsdata = aiaBackendClient.hentEndringIRegistreringdata(new EndringIRegistreringdataRequest(fnr));
            final RegistreringResponseDto oppdaterteRegistreringsData = oppdaterRegistreringsdataHvisNyeEndringer(registreringData, endringIRegistreringsdata);
            oyeblikksbildeRepository.upsertRegistreringOyeblikksbilde(vedtakId, oppdaterteRegistreringsData);
        }
        if (kilder.stream().anyMatch(kilde -> kilde.equals(VedtakOpplysningKilder.EGENVURDERING.getDesc()))) {
            final EgenvurderingResponseDTO egenvurdering = aiaBackendClient.hentEgenvurdering(new EgenvurderingForPersonRequest(fnr));
            OyeblikksbildeEgenvurderingDto egenvurderingData = mapToEgenvurderingData(egenvurdering);
            oyeblikksbildeRepository.upsertEgenvurderingOyeblikksbilde(vedtakId, egenvurderingData);
        }
    }

    public OyeblikksbildeEgenvurderingDto mapToEgenvurderingData(EgenvurderingResponseDTO egenvurderingResponseDTO) {//public for test
        List<OyeblikksbildeEgenvurderingDto.Svar> svar = new ArrayList<>();
        if (egenvurderingResponseDTO != null) {
            String svartekst = egenvurderingResponseDTO.getTekster().getSvar().get(egenvurderingResponseDTO.getOppfolging());
            svar.add(new OyeblikksbildeEgenvurderingDto.Svar(
                    egenvurderingResponseDTO.getTekster().getSporsmal(),
                    svartekst,
                    egenvurderingResponseDTO.getOppfolging(),
                    egenvurderingResponseDTO.getDialogId()
            ));
            return new OyeblikksbildeEgenvurderingDto(egenvurderingResponseDTO.getDato(), svar);
        }
        return new OyeblikksbildeEgenvurderingDto();
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
        } catch (Exception err) {
            log.error("Kunne ikke parse string til JSONObject");
            throw err;
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
