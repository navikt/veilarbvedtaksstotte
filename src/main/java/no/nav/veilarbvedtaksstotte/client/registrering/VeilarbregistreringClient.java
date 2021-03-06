package no.nav.veilarbvedtaksstotte.client.registrering;

import no.nav.common.health.HealthCheck;

public interface VeilarbregistreringClient extends HealthCheck {

    String hentRegistreringDataJson(String fnr);

    RegistreringData hentRegistreringData(String fnr);

}
