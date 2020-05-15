package no.nav.veilarbvedtaksstotte.client.api;

import no.nav.common.health.HealthCheck;
import no.nav.veilarbvedtaksstotte.domain.RegistreringData;

public interface RegistreringClient extends HealthCheck {

    String hentRegistreringDataJson(String fnr);

    RegistreringData hentRegistreringData(String fnr);

}
