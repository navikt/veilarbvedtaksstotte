package no.nav.veilarbvedtaksstotte.client.arbeidssoekeregisteret;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.Fnr;

public interface OppslagArbeidssoekerregisteretClient extends HealthCheck {
    OpplysningerOmArbeidssoekerMedProfilering hentSisteOpplysningerOmArbeidssoekerMedProfilering(Fnr fnr);
}
