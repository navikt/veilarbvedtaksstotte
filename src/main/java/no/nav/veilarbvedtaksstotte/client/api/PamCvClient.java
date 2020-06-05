package no.nav.veilarbvedtaksstotte.client.api;

import no.nav.common.health.HealthCheck;

public interface PamCvClient extends HealthCheck {

    String hentCV(String fnr);

}
