package no.nav.veilarbvedtaksstotte.client.arena;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.arena.dto.VeilarbArenaOppfolging;

import java.util.Optional;

public interface VeilarbarenaClient extends HealthCheck {

    Optional<VeilarbArenaOppfolging> hentOppfolgingsbruker(Fnr fnr);

    Optional<VeilarbArenaOppfolging> hentOppfolgingsbrukerUtenCache(Fnr fnr);

    Optional<String> oppfolgingssak(Fnr fnr);
}
