package no.nav.veilarbvedtaksstotte.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbArenaOppfolging;
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient;
import no.nav.veilarbvedtaksstotte.client.registrering.RegistreringData;
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClient;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.OppfolgingPeriodeDTO;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.veilarbvedtaksstotte.utils.OppfolgingUtils;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static no.nav.veilarbvedtaksstotte.config.ApplicationConfig.APPLICATION_NAME;
import static no.nav.veilarbvedtaksstotte.utils.EnumUtils.getName;
import static no.nav.veilarbvedtaksstotte.utils.TimeUtils.toLocalDateTime;
import static no.nav.veilarbvedtaksstotte.utils.VedtakUtils.tellVedtakEtterDato;

@Service
@Slf4j
@RequiredArgsConstructor
public class MetricsService {

    private final MetricsClient influxClient;

    private final VeilarboppfolgingClient oppfolgingClient;

    private final VeilarbarenaClient veilarbarenaClient;

    private final VeilarbregistreringClient registreringClient;

    private final VedtaksstotteRepository vedtaksstotteRepository;

    private final AktorOppslagClient aktorOppslagClient;


    private static Event createMetricEvent(String tagName) {
        return new Event(APPLICATION_NAME + ".metrikker." + tagName);
    }

    private static long localDateTimeToMillis(LocalDateTime ldt) {
        return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public void rapporterMetrikkerForFattetVedtak(Vedtak vedtak) {
        try {
            rapporterVedtakSendt(vedtak);
            rapporterTidFraRegistrering(vedtak);
            rapporterVedtakSendtSykmeldtUtenArbeidsgiver(vedtak);
        } catch (Exception e) {
            log.warn("Klarte ikke rapportere metrikker for fattet vedtak", e);
        }
    }

    private void rapporterVedtakSendt(Vedtak vedtak) {
        Event event = createMetricEvent("vedtak-sendt");
        long utkastOpprettetMillis = localDateTimeToMillis(vedtak.getUtkastOpprettet());
        long secondsUsed = (System.currentTimeMillis() - utkastOpprettetMillis) / 1000;
        int begrunnelseLengde = vedtak.getBegrunnelse() != null ? vedtak.getBegrunnelse().length() : 0;

        event.addFieldToReport("sekunderBrukt", secondsUsed);
        event.addFieldToReport("innsatsgruppe", getName(vedtak.getInnsatsgruppe()));
        event.addFieldToReport("enhetsId", vedtak.getOppfolgingsenhetId());
        event.addFieldToReport("begrunnelseLengde", begrunnelseLengde);

        if (vedtak.getHovedmal() != null) {
            event.addFieldToReport("hovedmal", vedtak.getHovedmal());
        }

        influxClient.report(event);
    }

    private void rapporterTidFraRegistrering(Vedtak vedtak) {
        long tidFraRegistrering = finnTidFraRegistreringStartet(AktorId.of(vedtak.getAktorId()));

        if (tidFraRegistrering < 0) return;

        long dagerFraRegistrering = TimeUnit.MILLISECONDS.toDays(tidFraRegistrering);

        Event event = createMetricEvent("tid-fra-registrering");
        event.addFieldToReport("innsatsgruppe", getName(vedtak.getInnsatsgruppe()));
        event.addFieldToReport("dager", dagerFraRegistrering);

        influxClient.report(event);
    }

    /**
     * Henter tid fra registrering startet fram til nå hvis brukeren kun har ett vedtak (det som nettopp ble sendt)
     * @param aktorId brukers aktør id
     * @return tid i millisekunder, -1 hvis det mangler data eller brukeren har mer enn ett vedtak i nåværende oppfølgingsperiode
     */
    private long finnTidFraRegistreringStartet(AktorId aktorId) {
        try {
            Fnr fnr = aktorOppslagClient.hentFnr(aktorId);
            List<Vedtak> vedtakTilBruker = vedtaksstotteRepository.hentFattedeVedtak(aktorId.get());
            RegistreringData registreringData = registreringClient.hentRegistreringData(fnr.get());
            Optional<ZonedDateTime> startDato = oppfolgingClient.hentGjeldendeOppfolgingsperiode(fnr)
                    .map(OppfolgingPeriodeDTO::getStartDato);

            if (startDato.isEmpty() || registreringData == null) {
                return -1;
            }

            if (tellVedtakEtterDato(vedtakTilBruker, toLocalDateTime(startDato.get())) == 1) {
                long registreringStart = localDateTimeToMillis(registreringData.registrering.opprettetDato);
                return localDateTimeToMillis(LocalDateTime.now()) - registreringStart;
            }
        } catch (Exception e) {
            log.error("Feil fra finnTidFraRegistreringStartet", e);
        }

        return -1;
    }

    private void rapporterVedtakSendtSykmeldtUtenArbeidsgiver(Vedtak vedtak) {
        boolean erSykmeldtUtenArbeidsgiver;

        try {
            Fnr fnr = aktorOppslagClient.hentFnr(AktorId.of(vedtak.getAktorId()));
            Optional<VeilarbArenaOppfolging> oppfolging = veilarbarenaClient.hentOppfolgingsbruker(fnr);
            erSykmeldtUtenArbeidsgiver =
                    oppfolging.map(VeilarbArenaOppfolging::getKvalifiseringsgruppekode)
                            .map(OppfolgingUtils::erSykmeldtUtenArbeidsgiver)
                            .orElse(false);
        } catch (Exception ignored) {
            erSykmeldtUtenArbeidsgiver = false;
        }

        if (erSykmeldtUtenArbeidsgiver) {
            try {
                Fnr fnr = aktorOppslagClient.hentFnr(AktorId.of(vedtak.getAktorId()));
                oppfolgingClient.hentGjeldendeOppfolgingsperiode(fnr)
                        .map(OppfolgingPeriodeDTO::getStartDato)
                        .ifPresent(startDato ->
                                rapporterVedtakSendtSykmeldtUtenArbeidsgiver(vedtak, startDato)
                        );
            } catch (Exception ignored) {}
        }
    }

    private void rapporterVedtakSendtSykmeldtUtenArbeidsgiver(Vedtak vedtak, ZonedDateTime oppfolgingStartDato) {
        LocalDate vedtakSendtDato = LocalDate.now();
        Long diff = Duration.between(vedtakSendtDato.atStartOfDay(), oppfolgingStartDato).toDays();
        Event event = createMetricEvent("sykmeldt-uten-arbeidsgiver-vedtak-sendt");
        event.addFieldToReport("dagerBrukt", diff);
        event.addFieldToReport("oppfolgingStartDato", toLocalDateTime(oppfolgingStartDato).toString());
        event.addFieldToReport("vedtakSendtDato", vedtakSendtDato.toString());
        event.addFieldToReport("enhetsId", vedtak.getOppfolgingsenhetId());
        influxClient.report(event);
    }

    public void rapporterUtkastSlettet() {
        influxClient.report(createMetricEvent("utkast-slettet"));
    }

    public void rapporterTidMellomUtkastOpprettetTilGodkjent(Vedtak vedtak) {
        Event event = createMetricEvent("tid-mellom-utkast-opprettet-til-godkjent");

        Long sekunderBrukt = Duration.between(vedtak.getUtkastOpprettet(), LocalDateTime.now()).getSeconds();
        event.addFieldToReport("sekunder", sekunderBrukt);

        influxClient.report(event);
    }

    public void repporterDialogMeldingSendtAvVeilederOgBeslutter(String melding, String sendtAv) {
        Event event = createMetricEvent("dialog-meldinger");

        int antallTegn = melding.length();
        event.addFieldToReport("sendtAv", sendtAv);
        event.addFieldToReport("antallTegn", antallTegn);

        influxClient.report(event);
    }
}
