package no.nav.fo.veilarbvedtaksstotte.service;

import no.nav.fo.veilarbvedtaksstotte.client.OppfolgingClient;
import no.nav.fo.veilarbvedtaksstotte.domain.OppfolgingPeriodeDTO;
import no.nav.fo.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.fo.veilarbvedtaksstotte.utils.OppfolgingUtils;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static no.nav.fo.veilarbvedtaksstotte.config.ApplicationConfig.APPLICATION_NAME;
import static no.nav.fo.veilarbvedtaksstotte.utils.EnumUtils.getName;

@Service
public class MetricsService {

    private OppfolgingClient oppfolgingClient;
    public static final int MILLISEC_IN_A_DAY = 24* 60 * 60 * 1000;

    @Inject
    public MetricsService (OppfolgingClient oppfolgingClient)  {
        this.oppfolgingClient = oppfolgingClient;
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

        event.addFieldToReport("sekunderBrukt", secondsUsed);
        event.addFieldToReport("innsatsgruppe", getName(vedtak.getInnsatsgruppe()));
        event.addFieldToReport("enhetsId", vedtak.getVeilederEnhetId());

        if (vedtak.getHovedmal() != null) {
            event.addFieldToReport("hovedmal", vedtak.getHovedmal());
        }

        event.report();
    }

    public void rapporterVedtakSendtSykmeldtUtenArbeidsgiver(String fnr) {
        String servicegruppe = oppfolgingClient.hentServicegruppe(fnr);
        if(OppfolgingUtils.erSykmeldtUtenArbeidsgiver(servicegruppe)){
            List<OppfolgingPeriodeDTO> oppfolgingPerioder = oppfolgingClient.hentOppfolgingPerioder(fnr);
            Date startDato = OppfolgingUtils.getOppfolgingStartDato(oppfolgingPerioder);
            Date vedtakSendtDato = new Date();
            String diff = Long.toString((vedtakSendtDato.getTime() - startDato.getTime())/MILLISEC_IN_A_DAY);

            Event event = createMetricEvent("sykmeldt-uten-arbeidsgiver-vedtak-sendt");
            event.addFieldToReport("dagerBrukt", diff);
            event.addFieldToReport("oppfolgingStartDato", startDato.toString());
            event.addFieldToReport("vedtakSendtDato", vedtakSendtDato.toString());
            event.report();
        }
    }

    public void rapporterUtkastSlettet() {
        createMetricEvent("utkast-slettet").report();
    }

}
