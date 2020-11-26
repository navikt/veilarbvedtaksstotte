package no.nav.veilarbvedtaksstotte.client.oppfolging;

import no.nav.common.health.HealthCheck;

public interface VeilarboppfolgingClient extends HealthCheck {

    String hentServicegruppe(String fnr);

    OppfolgingDTO hentOppfolgingData(String fnr);

}
