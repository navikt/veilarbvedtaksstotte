package no.nav.veilarbvedtaksstotte.client.arena;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.Fnr;

public interface VeilarbarenaClient extends HealthCheck {

    VeilarbArenaOppfolging hentOppfolgingsbruker(Fnr fnr);

    String oppfolgingssak(Fnr fnr);

}
