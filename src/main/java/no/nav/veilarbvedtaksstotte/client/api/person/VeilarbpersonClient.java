package no.nav.veilarbvedtaksstotte.client.api.person;

import no.nav.common.health.HealthCheck;

public interface VeilarbpersonClient extends HealthCheck {

    PersonNavn hentPersonNavn(String fnr);

    String hentCVOgJobbprofil(String fnr);

}
