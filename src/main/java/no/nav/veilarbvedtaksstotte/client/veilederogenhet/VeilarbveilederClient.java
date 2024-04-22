package no.nav.veilarbvedtaksstotte.client.veilederogenhet;

import no.nav.common.health.HealthCheck;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.Veileder;
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.dto.VeilederEnheterDTO;

public interface VeilarbveilederClient extends HealthCheck {

    String hentEnhetNavn(String enhetId);

    String hentVeilederNavn(String veilederIdent);
    Veileder hentVeileder(String veilederIdent);

    VeilederEnheterDTO hentInnloggetVeilederEnheter();

}
