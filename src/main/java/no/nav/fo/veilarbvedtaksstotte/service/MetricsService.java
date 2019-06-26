package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.fo.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static no.nav.fo.veilarbvedtaksstotte.config.ApplicationConfig.APPLICATION_NAME;
import static no.nav.fo.veilarbvedtaksstotte.utils.EnumUtils.getName;

@Service
public class MetricsService {

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

        event.addFieldToReport("sekunderBrukt", secondsUsed);
        event.addFieldToReport("innsatsgruppe", getName(vedtak.getInnsatsgruppe()));
        event.addFieldToReport("enhetsId", vedtak.getVeilederEnhetId());

        if (vedtak.getHovedmal() != null) {
            event.addFieldToReport("hovedmal", vedtak.getHovedmal());
        }

        event.report();
    }

    public void rapporterUtkastSlettet() {
        createMetricEvent("utkast-slettet").report();
    }

}
