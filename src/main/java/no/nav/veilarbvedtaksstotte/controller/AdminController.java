package no.nav.veilarbvedtaksstotte.controller;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.job.JobRunner;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarbvedtaksstotte.config.KafkaProperties;
import no.nav.veilarbvedtaksstotte.controller.dto.*;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.veilarbvedtaksstotte.service.AuthService;
import no.nav.veilarbvedtaksstotte.service.KafkaRepubliseringService;
import no.nav.veilarbvedtaksstotte.service.SakStatistikkService;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static no.nav.common.utils.EnvironmentUtils.isDevelopment;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    public static final String POAO_ADMIN = "poao-admin";

    private final AuthService authService;

    private final KafkaRepubliseringService kafkaRepubliseringService;

    private final VedtakService vedtakService;
    private final VedtaksstotteRepository vedtaksstotteRepository;
    private final KafkaProperties kafkaProperties;
    private final SakStatistikkService sakStatistikkService;
    private final AktorOppslagClient aktorOppslagClient;

    @Autowired
    public AdminController(AuthService authService,
                           KafkaRepubliseringService kafkaRepubliseringService,
                           VedtakService vedtakService, VedtaksstotteRepository vedtaksstotteRepository,
                           KafkaProperties kafkaProperties, SakStatistikkService sakStatistikkService, AktorOppslagClient aktorOppslagClient) {
        this.authService = authService;
        this.kafkaRepubliseringService = kafkaRepubliseringService;
        this.vedtakService = vedtakService;
        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.kafkaProperties = kafkaProperties;
        this.sakStatistikkService = sakStatistikkService;
        this.aktorOppslagClient = aktorOppslagClient;
    }

    @PostMapping("/republiser/siste-14a-vedtak")
    public String republiserSiste14aVedtak() {
        sjekkTilgangTilAdmin();
        return JobRunner.runAsync(
                "republiser-siste-14a-vedtak",
                kafkaRepubliseringService::republiserSiste14aVedtak
        );
    }

    @PostMapping("/republiser/vedtak-14a-fattet-dvh")
    public String republiserVedtak14aFattetDvh() {
        sjekkTilgangTilAdmin();
        return JobRunner.runAsync(
                "republiser-vedtak-14a-fattet-dvh",
                () -> kafkaRepubliseringService.republiserVedtak14aFattetDvh(100)
        );
    }

    @PostMapping("/republiser/vedtak-pa-kafka-topic")
    public String republiserVedtakPaKafkaTopic(@RequestBody RepubliserVedtakPaKafkaTopicRequest request) {
        sjekkTilgangTilAdmin();
        if (List.of(kafkaProperties.getSiste14aVedtakTopic(), kafkaProperties.getVedtakSendtTopic()).contains(request.getKafkaTopic())) {
            return JobRunner.runAsync(
                    "republiser-vedtak-pa-kafka-topic",
                    () -> kafkaRepubliseringService.republiserVedtakPaKafkaTopic(request)
            );
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ugyldig kafka-topic");
        }
    }

    @PostMapping("/republiser/vedtak-pa-bigquery")
    public String republiserVedtakPaBigQuery(@RequestBody RepubliserVedtakPaBigQueryRequest request) {
        sjekkTilgangTilAdmin();

        return JobRunner.runAsync(
                "republiser-vedtak-pa-bigquery",
                () -> request.getVedtaksIDer().forEach((vedtakId) ->
                        sakStatistikkService.sendFattetVedtak(
                                Long.parseLong(vedtakId),
                                vedtaksstotteRepository::hentVedtak,
                                aktorOppslagClient::hentFnr
                        )
                )
        );

    }

    @PostMapping("/republiser/sakstatistikkrad-pa-bigquery")
    public String republiserSakStatistikkRadPaBigQuery(@RequestBody RepubliserSakStatistikkRadPaBigQueryRequest request) {
        sjekkTilgangTilAdmin();

        return JobRunner.runAsync(
                "republiser-sakstatistikkrad-pa-bigquery",
                () -> sakStatistikkService.hentOgSendStatistikkRadTilBQ(request.getSekvensnummer())
        );
    }

    /**
     * OBS: Denne slettingen skal kun brukes ved personvernsbrudd.
     */
    @PutMapping("/slett-vedtak")
    public void slettVedtak(@RequestBody SlettVedtakRequest slettVedtakRequest) {
        sjekkTilgangTilAdmin();
        if (!isDevelopment().orElse(false)) {
            authService.erInnloggetBrukerModiaAdmin();
        }
        String regex = "[A-Za-z]\\d{6}";

        if (!slettVedtakRequest.getAnsvarligVeileder().get().matches(regex)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ansvarlig veileder må ha formatet X123456");
        }
        vedtakService.slettVedtak(slettVedtakRequest, NavIdent.of(authService.getInnloggetVeilederIdent()));
    }


    /**
     * OBS: Denne sladding skal kun brukes ved feil informasjon i beskrivelsen, og ved eksplisitt beskjed via en jira-sak.
     */
    @PutMapping("/sladd-vedtak")
    public void sladdVedtak(@RequestBody SladdVedtakRequest sladdVedtakRequest) {
        sjekkTilgangTilAdmin();
        if (!isDevelopment().orElse(false)) {
            authService.erInnloggetBrukerModiaAdmin();
        }
        String regex = "[A-Za-z]\\d{6}";

        if (!sladdVedtakRequest.getAnsvarligVeileder().get().matches(regex)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ansvarlig veileder må ha formatet X123456");
        }
        vedtakService.sladdVedtak(sladdVedtakRequest, NavIdent.of(authService.getInnloggetVeilederIdent()));
    }

    private void sjekkTilgangTilAdmin() {
        boolean erInternBruker = authService.erInternBruker();
        boolean erPoaoAdmin = POAO_ADMIN.equals(authService.hentApplikasjonFraContex());

        if (erPoaoAdmin && erInternBruker) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    @PostMapping("/aktoridSjekk")
    @Operation(
            summary = "Sjekk om vi har en aktørid i vedtakstabellen",
            description = "Sjekker om vi har en aktørid i vedtakstabellen." +
                    "Dette er i tilfeller ved merge/split og for å sjekke om vi er berørt."
    )
    public Boolean sjekkOmViHarAktorId(@RequestBody AktorIdRequestDTO aktorIdRequestDTO) {
        sjekkTilgangTilAdmin();
        return vedtaksstotteRepository.aktorIdFinnesIVedtakTabell(aktorIdRequestDTO.getAktorId().get());
    }

}
