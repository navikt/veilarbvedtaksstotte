package no.nav.veilarbvedtaksstotte.client.api.oppfolging;

import no.nav.common.health.HealthCheck;

public interface OppfolgingClient extends HealthCheck {

    String hentServicegruppe(String fnr);

    OppfolgingDTO hentOppfolgingData(String fnr);

}
