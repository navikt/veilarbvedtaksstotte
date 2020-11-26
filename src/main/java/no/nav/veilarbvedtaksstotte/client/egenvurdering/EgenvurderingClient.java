package no.nav.veilarbvedtaksstotte.client.egenvurdering;

import no.nav.common.health.HealthCheck;

public interface EgenvurderingClient extends HealthCheck {

    String hentEgenvurdering(String fnr);

}
