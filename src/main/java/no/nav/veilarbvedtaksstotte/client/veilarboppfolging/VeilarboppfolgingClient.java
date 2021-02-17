package no.nav.veilarbvedtaksstotte.client.veilarboppfolging;

import no.nav.common.health.HealthCheck;

import java.util.List;

public interface VeilarboppfolgingClient extends HealthCheck {

    OppfolgingsstatusDTO hentOppfolgingData(String fnr);

    List<OppfolgingPeriodeDTO> hentOppfolgingsperioder(String fnr);

}
