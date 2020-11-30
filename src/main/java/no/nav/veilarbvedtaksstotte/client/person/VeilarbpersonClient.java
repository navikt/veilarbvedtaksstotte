package no.nav.veilarbvedtaksstotte.client.person;

import no.nav.common.health.HealthCheck;

public interface VeilarbpersonClient extends HealthCheck {

    PersonNavn hentPersonNavn(String fnr);

    String hentCVOgJobbprofil(String fnr);

}
