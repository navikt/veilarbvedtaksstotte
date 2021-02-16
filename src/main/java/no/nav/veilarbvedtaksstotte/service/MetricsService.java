package no.nav.veilarbvedtaksstotte.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.veilarbvedtaksstotte.client.registrering.RegistreringData;
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClient;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.OppfolgingPeriodeDTO;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.veilarbvedtaksstotte.utils.OppfolgingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static no.nav.veilarbvedtaksstotte.config.ApplicationConfig.APPLICATION_NAME;
import static no.nav.veilarbvedtaksstotte.utils.EnumUtils.getName;
import static no.nav.veilarbvedtaksstotte.utils.VedtakUtils.tellVedtakEtterDato;

@Service
@Slf4j
public class MetricsService {

    private final MetricsClient influxClient;

    private final MeterRegistry meterRegistry;

    private final VeilarboppfolgingClient oppfolgingClient;

    private final VeilarbregistreringClient registreringClient;

    private final VedtaksstotteRepository vedtaksstotteRepository;

    @Autowired
    public MetricsService(MetricsClient influxClient,
                          MeterRegistry meterRegistry,
                          VeilarboppfolgingClient oppfolgingClient,
                          VeilarbregistreringClient registreringClient,
                          VedtaksstotteRepository vedtaksstotteRepository)  {
        this.influxClient = influxClient;
        this.meterRegistry = meterRegistry;
        this.oppfolgingClient = oppfolgingClient;
        this.registreringClient = registreringClient;
        this.vedtaksstotteRepository = vedtaksstotteRepository;
    }

    private static Event createMetricEvent(String tagName) {
        return new Event(APPLICATION_NAME + ".metrikker." + tagName);
    }

    private static long localDateTimeToMillis(LocalDateTime ldt) {
        return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public void rapporterSendDokument() {
        // Brukes sammen med metrikker for sendte vedtak for å verfisere at antall dokumenter opprettet er lik antall vedtak sendt
        influxClient.report(createMetricEvent("send-dokument"));
    }

    public void rapporterVedtakSendt(Vedtak vedtak) {
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

    public void rapporterTidFraRegistrering(Vedtak vedtak, String aktorId, String fnr) {
        long tidFraRegistrering = finnTidFraRegistreringStartet(aktorId, fnr);

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
     * @param fnr brukers fødselsnummer
     * @return tid i millisekunder, -1 hvis det mangler data eller brukeren har mer enn ett vedtak i nåværende oppfølgingsperiode
     */
    private long finnTidFraRegistreringStartet(String aktorId, String fnr) {
        try {
            List<Vedtak> vedtakTilBruker = vedtaksstotteRepository.hentFattedeVedtak(aktorId);
            RegistreringData registreringData = registreringClient.hentRegistreringData(fnr);
            List<OppfolgingPeriodeDTO> perioder = oppfolgingClient.hentOppfolgingsperioder(fnr);
            Optional<LocalDateTime> startDato = OppfolgingUtils.getOppfolgingStartDato(perioder);

            if (startDato.isEmpty() || registreringData == null) {
                return -1;
            }

            if (tellVedtakEtterDato(vedtakTilBruker, startDato.get()) == 1) {
                long registreringStart = localDateTimeToMillis(registreringData.registrering.opprettetDato);
                return localDateTimeToMillis(LocalDateTime.now()) - registreringStart;
            }
        } catch (Exception e) {
            log.error("Feil fra finnTidFraRegistreringStartet", e);
        }

        return -1;
    }

    public void rapporterVedtakSendtSykmeldtUtenArbeidsgiver(Vedtak vedtak, String fnr) {
        boolean erSykmeldtMedArbeidsgiver;

        try {
            String serviceGruppe = oppfolgingClient.hentOppfolgingData(fnr).getServicegruppe();
            erSykmeldtMedArbeidsgiver = OppfolgingUtils.erSykmeldtUtenArbeidsgiver(serviceGruppe);
        } finally {}

        if (erSykmeldtMedArbeidsgiver) {
            try {
                List<OppfolgingPeriodeDTO> data = oppfolgingClient.hentOppfolgingsperioder(fnr);
                Optional<LocalDateTime> oppolgingStartDato = OppfolgingUtils.getOppfolgingStartDato(data);
                oppolgingStartDato.ifPresent(
                        startDato -> rapporterVedtakSendtSykmeldtUtenArbeidsgiver(vedtak, startDato)
                );
            } catch (Exception ignored) {}
        }

    }

    private void rapporterVedtakSendtSykmeldtUtenArbeidsgiver(Vedtak vedtak, LocalDateTime oppfolgingStartDato) {
        LocalDate vedtakSendtDato = LocalDate.now();
        Long diff = Duration.between(vedtakSendtDato.atStartOfDay(), oppfolgingStartDato).toDays();
        Event event = createMetricEvent("sykmeldt-uten-arbeidsgiver-vedtak-sendt");
        event.addFieldToReport("dagerBrukt", diff);
        event.addFieldToReport("oppfolgingStartDato", oppfolgingStartDato.toString());
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

    public void rapporterFeilendeFerdigstillingAvJournalpost() {
        meterRegistry.counter("feilende_ferdigstilling_av_journalpost").increment();
    }

    public void rapporterFeilendeDistribusjonAvJournalpost() {
        meterRegistry.counter("feilende_distribusjon_av_journalpost").increment();
    }
}
