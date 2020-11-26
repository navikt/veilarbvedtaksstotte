package no.nav.veilarbvedtaksstotte.client.arena;

import no.nav.common.health.HealthCheck;

public interface ArenaClient extends HealthCheck {

    String oppfolgingsenhet(String fnr);

}
