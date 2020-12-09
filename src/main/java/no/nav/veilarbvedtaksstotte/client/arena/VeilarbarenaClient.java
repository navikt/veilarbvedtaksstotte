package no.nav.veilarbvedtaksstotte.client.arena;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;

public interface VeilarbarenaClient extends HealthCheck {

    EnhetId oppfolgingsenhet(Fnr fnr);

    String oppfolgingssak(Fnr fnr);

}
