package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.fo.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;

import static no.nav.fo.veilarbvedtaksstotte.utils.EnumUtils.getName;

@Service
public class MetricsService {

    private final static String APP_NAME = "veilarbvedtaksstotte";

    private static Event createEvent(String... tagSegments) {
        final String tagName = Arrays.stream(tagSegments).reduce(APP_NAME, (acc, str) -> acc + "." + str);
        return MetricsFactory.createEvent(tagName);
    }

    private static long localDateTimeToMillis(LocalDateTime ldt) {
        return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public void rapporterVedtakSendt(Vedtak vedtak) {
        Event event = createEvent("vedtak", "sendt");
        long utkastOpprettetMillis = localDateTimeToMillis(vedtak.getUtkastOpprettet());
        long minutesUsed = (System.currentTimeMillis() - utkastOpprettetMillis) / 60;
        event.addFieldToReport("minutterBrukt", minutesUsed);
        event.addFieldToReport("innsatsgruppe", getName(vedtak.getInnsatsgruppe()));
        event.addFieldToReport("hovedmaal", vedtak.getHovedmal());
        event.addFieldToReport("enhetsId", vedtak.getVeilederEnhetId());
        event.report();
    }

    public void rapporterUtkastSlettet() {
        createEvent("utkast", "slettet").report();
    }

}
