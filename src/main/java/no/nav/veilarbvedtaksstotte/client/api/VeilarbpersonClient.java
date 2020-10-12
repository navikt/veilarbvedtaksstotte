package no.nav.veilarbvedtaksstotte.client.api;

import no.nav.common.health.HealthCheck;
import no.nav.veilarbvedtaksstotte.domain.PersonNavn;

public interface VeilarbpersonClient extends HealthCheck {

    PersonNavn hentPersonNavn(String fnr);

    String hentCVOgJobbprofil(String fnr);

}
