package no.nav.veilarbvedtaksstotte.client.api;

import no.nav.common.health.HealthCheck;
import no.nav.veilarbvedtaksstotte.domain.PersonNavn;

public interface PersonClient extends HealthCheck {

    PersonNavn hentPersonNavn(String fnr);

}
