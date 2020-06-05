package no.nav.veilarbvedtaksstotte.client.api;

import no.nav.common.health.HealthCheck;
import no.nav.veilarbvedtaksstotte.domain.Veileder;
import no.nav.veilarbvedtaksstotte.domain.VeilederEnheterDTO;

public interface VeiledereOgEnhetClient extends HealthCheck {

    String hentEnhetNavn(String enhetId);

    Veileder hentVeileder(String veilederIdent);

    VeilederEnheterDTO hentInnloggetVeilederEnheter();

}
