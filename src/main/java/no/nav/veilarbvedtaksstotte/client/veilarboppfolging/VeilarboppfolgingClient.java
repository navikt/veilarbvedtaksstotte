package no.nav.veilarbvedtaksstotte.client.veilarboppfolging;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.Fnr;

import java.util.List;

public interface VeilarboppfolgingClient extends HealthCheck {

    List<OppfolgingPeriodeDTO> hentOppfolgingsperioder(Fnr fnr);

}
