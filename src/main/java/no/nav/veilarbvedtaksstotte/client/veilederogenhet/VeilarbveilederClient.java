package no.nav.veilarbvedtaksstotte.client.veilederogenhet;

import no.nav.common.health.HealthCheck;

public interface VeilarbveilederClient extends HealthCheck {

    String hentEnhetNavn(String enhetId);

    Veileder hentVeileder(String veilederIdent);

    VeilederEnheterDTO hentInnloggetVeilederEnheter();

}
