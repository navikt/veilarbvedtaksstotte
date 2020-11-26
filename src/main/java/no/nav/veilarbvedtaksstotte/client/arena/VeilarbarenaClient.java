package no.nav.veilarbvedtaksstotte.client.arena;

import no.nav.common.health.HealthCheck;

public interface VeilarbarenaClient extends HealthCheck {

    String oppfolgingsenhet(String fnr);

}
