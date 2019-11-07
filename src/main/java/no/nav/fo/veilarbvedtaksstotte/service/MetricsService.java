package no.nav.fo.veilarbvedtaksstotte.service;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.client.OppfolgingClient;
import no.nav.fo.veilarbvedtaksstotte.client.RegistreringClient;
import no.nav.fo.veilarbvedtaksstotte.domain.OppfolgingPeriodeDTO;
import no.nav.fo.veilarbvedtaksstotte.domain.RegistreringData;
import no.nav.fo.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.fo.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import no.nav.fo.veilarbvedtaksstotte.utils.OppfolgingUtils;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static no.nav.fo.veilarbvedtaksstotte.config.ApplicationConfig.APPLICATION_NAME;
import static no.nav.fo.veilarbvedtaksstotte.utils.EnumUtils.getName;
import static no.nav.fo.veilarbvedtaksstotte.utils.VedtakUtils.tellVedtakEtterDato;

@Service
@Slf4j
public class MetricsService {

    private OppfolgingClient oppfolgingClient;

    private RegistreringClient registreringClient;

    private VedtaksstotteRepository vedtaksstotteRepository;

    @Inject
    public MetricsService(OppfolgingClient oppfolgingClient, RegistreringClient registreringClient, VedtaksstotteRepository vedtaksstotteRepository)  {
        this.oppfolgingClient = oppfolgingClient;
        this.registreringClient = registreringClient;
        this.vedtaksstotteRepository = vedtaksstotteRepository;
    }

    private static Event createMetricEvent(String tagName) {
        return MetricsFactory.createEvent(APPLICATION_NAME + ".metrikker." + tagName);
    }

    private static long localDateTimeToMillis(LocalDateTime ldt) {
        return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public void rapporterVedtakSendt(Vedtak vedtak) {
        Event event = createMetricEvent("vedtak-sendt");
        long utkastOpprettetMillis = localDateTimeToMillis(vedtak.getUtkastOpprettet());
        long secondsUsed = (System.currentTimeMillis() - utkastOpprettetMillis) / 1000;
        int begrunnelseLengde = vedtak.getBegrunnelse() != null ? vedtak.getBegrunnelse().length() : 0;

        event.addFieldToReport("sekunderBrukt", secondsUsed);
        event.addFieldToReport("innsatsgruppe", getName(vedtak.getInnsatsgruppe()));
        event.addFieldToReport("enhetsId", vedtak.getVeilederEnhetId());
        event.addFieldToReport("begrunnelseLengde", begrunnelseLengde);

        if (vedtak.getHovedmal() != null) {
            event.addFieldToReport("hovedmal", vedtak.getHovedmal());
        }

        event.report();
    }

    public void rapporterTidFraRegistrering(Vedtak vedtak, String aktorId, String fnr) {
        long tidFraRegistrering = finnTidFraRegistreringStartet(aktorId, fnr);

        if (tidFraRegistrering < 0) return;

        long dagerFraRegistrering = TimeUnit.MILLISECONDS.toDays(tidFraRegistrering);

        Event event = createMetricEvent("tid-fra-registrering");
        event.addFieldToReport("innsatsgruppe", getName(vedtak.getInnsatsgruppe()));
        event.addFieldToReport("dager", dagerFraRegistrering);
        event.report();
    }

    /**
     * Henter tid fra registrering startet fram til nå hvis brukeren kun har ett vedtak (det som nettopp ble sendt)
     * @param aktorId brukers aktør id
     * @param fnr brukers fødselsnummer
     * @return tid i millisekunder, -1 hvis det mangler data eller brukeren har mer enn ett vedtak i nåværende oppfølgingsperiode
     */
    private long finnTidFraRegistreringStartet(String aktorId, String fnr) {
        try {
            List<Vedtak> vedtakTilBruker = vedtaksstotteRepository.hentVedtak(aktorId);
            RegistreringData registreringData = registreringClient.hentRegistreringData(fnr);
            List<OppfolgingPeriodeDTO> perioder = oppfolgingClient.hentOppfolgingsPerioder(fnr);
            Optional<LocalDate> startDato = OppfolgingUtils.getOppfolgingStartDato(perioder);

            if (!startDato.isPresent() || registreringData == null) {
                return -1;
            }

            if (tellVedtakEtterDato(vedtakTilBruker, startDato.get()) <= 1) {
                long registreringStart = localDateTimeToMillis(registreringData.registrering.opprettetDato);
                return localDateTimeToMillis(LocalDateTime.now()) - registreringStart;
            }
        } catch (Exception e) {
            log.error("Feil fra finnTidFraRegistreringStartet", e);
        }

        return -1;
    }

    public void rapporterVedtakSendtSykmeldtUtenArbeidsgiver(Vedtak vedtak, String fnr) {
        boolean erSykmeldtMedArbeidsgiver = Try.of(() -> oppfolgingClient.hentServicegruppe(fnr))
                .map(OppfolgingUtils::erSykmeldtUtenArbeidsgiver)
                .getOrElse(false);

        if (erSykmeldtMedArbeidsgiver) {
            try {
                List<OppfolgingPeriodeDTO> data = oppfolgingClient.hentOppfolgingsPerioder(fnr);
                Optional<LocalDate> dato = OppfolgingUtils.getOppfolgingStartDato(data);
                dato.ifPresent(localDate -> rapporterVedtakSendtSykmeldtUtenArbeidsgiver(vedtak, localDate));
            } catch (Exception ignored) {}
        }

    }

    private void rapporterVedtakSendtSykmeldtUtenArbeidsgiver(Vedtak vedtak, LocalDate dato) {
        LocalDate vedtakSendtDato = LocalDate.now();
        Long diff = Duration.between(vedtakSendtDato.atStartOfDay(), dato.atStartOfDay()).toDays();
        Event event = createMetricEvent("sykmeldt-uten-arbeidsgiver-vedtak-sendt");
        event.addFieldToReport("dagerBrukt", diff);
        event.addFieldToReport("oppfolgingStartDato", dato.toString());
        event.addFieldToReport("vedtakSendtDato", vedtakSendtDato.toString());
        event.addFieldToReport("enhetsId", vedtak.getVeilederEnhetId());
        event.report();
    }

    public void rapporterUtkastSlettet() {
        createMetricEvent("utkast-slettet").report();
    }

}
