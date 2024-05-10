package no.nav.veilarbvedtaksstotte.client.registrering;

import no.nav.common.health.HealthCheck;
import no.nav.veilarbvedtaksstotte.client.registrering.dto.RegistreringResponseDto;

public interface VeilarbregistreringClient extends HealthCheck {
    RegistreringResponseDto hentRegistreringData(String fnr);

}
