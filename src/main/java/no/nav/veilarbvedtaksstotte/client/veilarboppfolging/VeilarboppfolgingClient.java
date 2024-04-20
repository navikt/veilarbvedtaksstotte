package no.nav.veilarbvedtaksstotte.client.veilarboppfolging;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.dto.OppfolgingPeriodeDTO;

import java.util.List;
import java.util.Optional;

public interface VeilarboppfolgingClient extends HealthCheck {

    Optional<OppfolgingPeriodeDTO> hentGjeldendeOppfolgingsperiode(Fnr fnr);

    List<OppfolgingPeriodeDTO> hentOppfolgingsperioder(Fnr fnr);

}
