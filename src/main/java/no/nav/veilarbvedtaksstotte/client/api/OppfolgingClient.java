package no.nav.veilarbvedtaksstotte.client.api;

import no.nav.common.health.HealthCheck;
import no.nav.veilarbvedtaksstotte.domain.OppfolgingDTO;

public interface OppfolgingClient extends HealthCheck {

    String hentServicegruppe(String fnr);

    OppfolgingDTO hentOppfolgingData(String fnr);

}
