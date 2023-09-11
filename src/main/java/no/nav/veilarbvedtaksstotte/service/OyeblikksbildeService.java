package no.nav.veilarbvedtaksstotte.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarbvedtaksstotte.client.aiaBackend.*;
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient;
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClient;
import no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.Oyeblikksbilde;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.OyeblikksbildeRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.veilarbvedtaksstotte.utils.JsonUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static no.nav.common.json.JsonUtils.toJson;
import static no.nav.veilarbvedtaksstotte.domain.oyeblikksbilde.OyeblikksbildeType.*;


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
        final EndringIRegistreringsdataResponse endringIRegistreringsdata = aiaBackendClient.hentEndringIRegistreringdata(new EndringIRegistreringdataRequest(fnr));
        final String oppdaterteRegistreringsData = oppdaterRegistreringsdataHvisNyeEndringer(registreringData, endringIRegistreringsdata);
        final EgenvurderingResponseDTO egenvurdering = aiaBackendClient.hentEgenvurdering(new EgenvurderingForPersonDTO(fnr));
        final String egenvurderingData = mapToEgenvurderingDataJson(egenvurdering);

        List<Oyeblikksbilde> oyeblikksbilde = Arrays.asList(
                new Oyeblikksbilde(vedtakId, CV_OG_JOBBPROFIL, cvOgJobbprofilData),
                new Oyeblikksbilde(vedtakId, REGISTRERINGSINFO, oppdaterteRegistreringsData),
                new Oyeblikksbilde(vedtakId, EGENVURDERING, egenvurderingData)
        );

        oyeblikksbilde.forEach(oyeblikksbildeRepository::upsertOyeblikksbilde);
    }

    public String mapToEgenvurderingDataJson(EgenvurderingResponseDTO egenvurderingResponseDTO) {//public for test
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

    public String oppdaterRegistreringsdataHvisNyeEndringer(String registreringsData, EndringIRegistreringsdataResponse endringIRegistreringsdata) { //public for test
	    if(registreringsData == null || endringIRegistreringsdata == null || endringIRegistreringsdata.getErBesvarelsenEndret() == null || !endringIRegistreringsdata.getErBesvarelsenEndret()) {
            return registreringsData;
        }

        try {
            JSONObject fullRegistreringData = new JSONObject(registreringsData);
            JSONObject registreringJson = (JSONObject) fullRegistreringData.get("registrering");
            registreringJson.put("endretAv", endringIRegistreringsdata.getEndretAv());
            registreringJson.put("endretTidspunkt", endringIRegistreringsdata.getEndretTidspunkt());

            JSONObject besvarelseJson = (JSONObject) registreringJson.get("besvarelse");
            besvarelseJson.put("dinSituasjon", endringIRegistreringsdata.getBesvarelse().getDinSituasjon().getVerdi());
            registreringJson.put("besvarelse", besvarelseJson);

            JSONArray teksterForBesvarelse = (JSONArray) registreringJson.get("teksterForBesvarelse");
            teksterForBesvarelse.forEach(t -> {
                JSONObject tekstForBesvarelse = (JSONObject) t;
                if (tekstForBesvarelse.get("sporsmalId").equals("dinSituasjon")) {
                    tekstForBesvarelse.put("svar", mapDinSituasjonVerdiToTekst(endringIRegistreringsdata.getBesvarelse().getDinSituasjon().getVerdi()));
                }
            });
            registreringJson.put("teksterForBesvarelse", teksterForBesvarelse);

            fullRegistreringData.put("registrering", registreringJson);

            return fullRegistreringData.toString();
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
